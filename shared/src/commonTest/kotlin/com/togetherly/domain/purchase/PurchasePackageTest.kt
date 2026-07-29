package com.togetherly.domain.purchase

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PurchasePackageTest {

    @Test
    fun validMonthlyPackageIsAccepted() {
        val pack = PurchasePackage(
            productId = ProductId("family_plus_monthly"),
            type = PurchasePackageType.MONTHLY,
            title = "Monthly",
            formattedPrice = "$4.99/month",
            billingPeriod = BillingPeriod.MONTH,
            offeringIdentifier = "default",
        )

        assertEquals(BillingPeriod.MONTH, pack.billingPeriod)
    }

    @Test
    fun validAnnualPackageIsAccepted() {
        val pack = PurchasePackage(
            productId = ProductId("family_plus_annual"),
            type = PurchasePackageType.ANNUAL,
            title = "Annual",
            formattedPrice = "$39.99/year",
            billingPeriod = BillingPeriod.YEAR,
            offeringIdentifier = "default",
        )

        assertEquals(BillingPeriod.YEAR, pack.billingPeriod)
    }

    @Test
    fun validLifetimePackageIsAccepted() {
        val pack = PurchasePackage(
            productId = ProductId("family_plus_lifetime"),
            type = PurchasePackageType.LIFETIME,
            title = "Lifetime",
            formattedPrice = "$79.99",
            billingPeriod = null,
            offeringIdentifier = "default",
        )

        assertEquals(null, pack.billingPeriod)
    }

    @Test
    fun monthlyPackageWithYearlyPeriodIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            PurchasePackage(
                productId = ProductId("family_plus_monthly"),
                type = PurchasePackageType.MONTHLY,
                title = "Monthly",
                formattedPrice = "$4.99/month",
                billingPeriod = BillingPeriod.YEAR,
                offeringIdentifier = "default",
            )
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, exception.reason)
    }

    @Test
    fun annualPackageWithMonthlyPeriodIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            PurchasePackage(
                productId = ProductId("family_plus_annual"),
                type = PurchasePackageType.ANNUAL,
                title = "Annual",
                formattedPrice = "$39.99/year",
                billingPeriod = BillingPeriod.MONTH,
                offeringIdentifier = "default",
            )
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, exception.reason)
    }

    @Test
    fun lifetimePackageWithBillingPeriodIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            PurchasePackage(
                productId = ProductId("family_plus_lifetime"),
                type = PurchasePackageType.LIFETIME,
                title = "Lifetime",
                formattedPrice = "$79.99",
                billingPeriod = BillingPeriod.MONTH,
                offeringIdentifier = "default",
            )
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, exception.reason)
    }

    @Test
    fun blankTitleIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            PurchasePackage(
                productId = ProductId("family_plus_monthly"),
                type = PurchasePackageType.MONTHLY,
                title = "",
                formattedPrice = "$4.99/month",
                billingPeriod = BillingPeriod.MONTH,
                offeringIdentifier = "default",
            )
        }
        assertEquals(DomainValidationReason.BLANK_VALUE, exception.reason)
    }

    @Test
    fun blankFormattedPriceIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            PurchasePackage(
                productId = ProductId("family_plus_monthly"),
                type = PurchasePackageType.MONTHLY,
                title = "Monthly",
                formattedPrice = "",
                billingPeriod = BillingPeriod.MONTH,
                offeringIdentifier = "default",
            )
        }
        assertEquals(DomainValidationReason.BLANK_VALUE, exception.reason)
    }
}
