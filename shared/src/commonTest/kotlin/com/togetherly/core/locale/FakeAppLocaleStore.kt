package com.togetherly.core.locale

class FakeAppLocaleStore(initial: String? = null) : AppLocaleStore {
    private var selected: String? = initial

    override fun getSelectedLanguage(): String? = selected

    override fun setSelectedLanguage(code: String?) {
        selected = code
    }
}
