package com.togetherly.data.purchase

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.logging.AppLogger
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchaseError
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.PurchaseStartupState
import com.togetherly.domain.purchase.RestoreResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val LOG_TAG = "RevenueCat"

/**
 * The only class in this codebase that touches `Purchases.sharedInstance` — everything above
 * [RevenueCatDataSource] depends only on that interface. Every method checks [isReady] first
 * ([RevenueCatConfigurator.state] is the single source of truth for whether `Purchases.configure`
 * ever actually succeeded): calling into an unconfigured SDK would either crash or return
 * meaningless data, so this class fails safely into a typed [com.togetherly.domain.purchase.PurchaseError.ConfigurationProblem]
 * instead, keeping free-mode behavior intact rather than risking a crash.
 */
class DefaultRevenueCatDataSource(
    private val configurator: RevenueCatConfigurator,
    private val clock: AppClock,
    private val logger: AppLogger,
    private val diagnostics: OperationalDiagnostics,
) : RevenueCatDataSource {

    override fun isReady(): Boolean = configurator.state.value is PurchaseStartupState.Ready

    override fun observeCustomerAccess(): Flow<AccessSnapshot> = callbackFlow {
        if (!isReady()) {
            close()
            return@callbackFlow
        }

        val delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                trySend(customerInfo.toCustomerSnapshot().toAccessSnapshot(clock.now()))
            }

            // Togetherly sells no promotional App Store/Play Store products — never starting the
            // purchase here is the correct no-op, not an oversight.
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: ((PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit) -> Unit,
            ) = Unit
        }
        Purchases.sharedInstance.delegate = delegate

        awaitClose { Purchases.sharedInstance.delegate = null }
    }

    override suspend fun getCustomerAccess(): DataResult<AccessSnapshot> {
        if (!isReady()) return notReadyResult()
        return runCatchingRevenueCat {
            Purchases.sharedInstance.awaitCustomerInfo().toCustomerSnapshot().toAccessSnapshot(clock.now())
        }
    }

    override suspend fun getOfferingPackages(): DataResult<List<PurchasePackage>> {
        if (!isReady()) return notReadyResult()
        return runCatchingRevenueCat {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val offering = offerings.current ?: offerings.all[DEFAULT_OFFERING_ID]
            val packages = offering?.toPurchasePackages().orEmpty()
            if (packages.isEmpty()) {
                logger.warn(LOG_TAG, "No available packages in the current offering.")
                throw NoPackagesAvailableException
            }
            val availableCount = offering?.availablePackages?.size ?: 0
            if (packages.size < availableCount) {
                // At least one package silently failed to map (toPurchasePackageOrNull returned
                // null) — an unexpected offering/product-configuration mismatch, never a reason to
                // fail the whole offering (the family still sees whichever packages did map).
                diagnostics.captureHandledException(
                    UnexpectedPurchaseMappingException,
                    DiagnosticContext(mapOf("feature" to "purchase", "operation" to "offering_package_mapping")),
                )
            }
            packages
        }
    }

    override suspend fun purchase(productId: ProductId): PurchaseResult {
        if (!isReady()) return PurchaseResult.Failure(PurchaseError.ConfigurationProblem)

        val packageToPurchase = try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val offering = offerings.current ?: offerings.all[DEFAULT_OFFERING_ID]
            offering?.availablePackages?.find { it.storeProduct.id == productId.value }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.error(LOG_TAG, "Could not load offerings before purchasing.", throwable)
            diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "purchase", "operation" to "offering_load_before_purchase")))
            null
        } ?: return PurchaseResult.Failure(PurchaseError.ProductUnavailable)

        return try {
            val result = Purchases.sharedInstance.awaitPurchase(packageToPurchase = packageToPurchase)
            val access = result.customerInfo.toCustomerSnapshot().toFamilyAccess()
            if (access.isPlus) {
                PurchaseResult.Success(access)
            } else {
                // The store transaction completed, but family_plus never activated — never trust
                // a completed transaction alone (see this file's own KDoc and the task spec).
                logger.warn(LOG_TAG, "Purchase completed but family_plus did not activate.")
                PurchaseResult.Failure(PurchaseError.Unknown)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (transactionException: PurchasesTransactionException) {
            when {
                transactionException.userCancelled -> PurchaseResult.Cancelled
                transactionException.code == PurchasesErrorCode.PaymentPendingError ->
                    PurchaseResult.Pending(productId)
                else -> PurchaseResult.Failure(transactionException.code.toPurchaseError())
            }
        } catch (throwable: Throwable) {
            logger.error(LOG_TAG, "Purchase failed.", throwable)
            diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "purchase", "operation" to "purchase")))
            PurchaseResult.Failure(PurchaseError.Unknown)
        }
    }

    override suspend fun restorePurchases(): RestoreResult {
        if (!isReady()) return RestoreResult.Failure(PurchaseError.ConfigurationProblem)

        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            RestoreResult.Success(customerInfo.toCustomerSnapshot().toAccessSnapshot(clock.now()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (purchasesException: PurchasesException) {
            RestoreResult.Failure(purchasesException.code.toPurchaseError())
        } catch (throwable: Throwable) {
            logger.error(LOG_TAG, "Restore failed.", throwable)
            diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "purchase", "operation" to "restore")))
            RestoreResult.Failure(PurchaseError.Unknown)
        }
    }

    /**
     * Never throws — a RevenueCat SDK failure here must never break the calling flow (the analytics
     * linker, or a feature ViewModel setting a customer attribute), only ever get logged. A no-op
     * when [isReady] is false, same as every other method in this class.
     */
    override fun setPostHogDistinctId(distinctId: String?) {
        setCustomerAttributes(mapOf(POSTHOG_USER_ID_ATTRIBUTE_KEY to distinctId))
    }

    override fun setCustomerAttributes(attributes: Map<String, String?>) {
        if (!isReady()) return
        try {
            Purchases.sharedInstance.setAttributes(attributes)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logger.error(LOG_TAG, "Failed to set RevenueCat customer attributes.", throwable)
        }
    }

    private fun <T> notReadyResult(): DataResult<T> = DataResult.Error(AppError.Purchase(PurchaseError.ConfigurationProblem))

    private suspend inline fun <T> runCatchingRevenueCat(crossinline block: suspend () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (purchasesException: PurchasesException) {
        DataResult.Error(AppError.Purchase(purchasesException.code.toPurchaseError(), purchasesException))
    } catch (throwable: Throwable) {
        logger.error(LOG_TAG, "RevenueCat call failed.", throwable)
        diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "purchase", "operation" to "revenuecat_call")))
        DataResult.Error(AppError.Purchase(PurchaseError.Unknown, throwable))
    }
}

private object NoPackagesAvailableException : Exception("No available packages in the current offering.")
private object UnexpectedPurchaseMappingException : Exception("One or more offering packages failed to map to a PurchasePackage.")
