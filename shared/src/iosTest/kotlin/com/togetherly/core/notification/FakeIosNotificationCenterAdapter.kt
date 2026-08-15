package com.togetherly.core.notification

internal class FakeIosNotificationCenterAdapter(
    var authorizationStatusValue: NotificationPermissionState = NotificationPermissionState.Granted,
    var requestAuthorizationResult: Boolean = true,
    private val addFailurePredicate: (IosReminderRequest) -> Boolean = { false },
) : IosNotificationCenterAdapter {

    private val pendingRequests = linkedMapOf<String, IosReminderRequest>()

    val authorizationStatusCalls: Int get() = _authorizationStatusCalls
    val requestAuthorizationCalls: Int get() = _requestAuthorizationCalls
    val addCalls: Int get() = _addCalls
    val removeCalls: Int get() = _removeCalls
    val addedRequests: List<IosReminderRequest> get() = _addedRequests.toList()

    private var _authorizationStatusCalls = 0
    private var _requestAuthorizationCalls = 0
    private var _addCalls = 0
    private var _removeCalls = 0
    private val _addedRequests = mutableListOf<IosReminderRequest>()

    override suspend fun authorizationStatus(): NotificationPermissionState {
        _authorizationStatusCalls += 1
        return authorizationStatusValue
    }

    override suspend fun requestAuthorization(): Boolean {
        _requestAuthorizationCalls += 1
        return requestAuthorizationResult
    }

    override suspend fun add(request: IosReminderRequest) {
        _addCalls += 1
        if (addFailurePredicate(request)) {
            throw IllegalStateException("Simulated add failure for ${request.identifier}")
        }
        pendingRequests[request.identifier] = request
        _addedRequests += request
    }

    override fun removePendingRequests(identifiers: List<String>) {
        _removeCalls += 1
        identifiers.forEach(pendingRequests::remove)
    }

    override suspend fun pendingRequestIdentifiers(): List<String> = pendingRequests.keys.toList()
}
