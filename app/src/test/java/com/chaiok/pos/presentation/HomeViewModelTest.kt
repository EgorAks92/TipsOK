package com.chaiok.pos.presentation

import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.presentation.home.HomeViewModel
import com.chaiok.pos.util.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `link card dialog shown once per session`() = runTest {
        val profileFlow = MutableStateFlow(WaiterProfile("1", "A", "B", "На смене", false, null))
        val settingsFlow = MutableStateFlow(AppSettings(false, false, "default"))

        val vm = HomeViewModel(
            observeProfileUseCase = ObserveProfileUseCase(FakeObserveWaiterRepo(profileFlow)),
            observeSettingsUseCase = ObserveSettingsUseCase(FakeSettingsRepo(settingsFlow)),
            logoutUseCase = LogoutUseCase(FakeAuthRepo2(), FakeSessionRepo2())
        )

        assertTrue(vm.uiState.value.showLinkCardDialog)
        vm.dismissLinkCardDialog()
        assertFalse(vm.uiState.value.showLinkCardDialog)

        profileFlow.value = profileFlow.value.copy(status = "Перерыв")
        assertFalse(vm.uiState.value.showLinkCardDialog)
    }
}
