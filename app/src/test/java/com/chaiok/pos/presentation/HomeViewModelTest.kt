package com.chaiok.pos.presentation

import com.chaiok.pos.data.repository.InMemorySessionRepository
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.presentation.home.HomeViewModel
import com.chaiok.pos.util.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `profile updates are reflected on home screen`() = runTest {
        val profile = WaiterProfile("1", "A", "B", "На смене", false, null)
        val profileFlow = MutableStateFlow<WaiterProfile?>(profile)
        val settingsFlow = MutableStateFlow(AppSettings(false, false, "default"))

        val vm = HomeViewModel(
            observeProfileUseCase = ObserveProfileUseCase(FakeObserveWaiterRepo(profileFlow)),
            observeSettingsUseCase = ObserveSettingsUseCase(FakeSettingsRepo(settingsFlow)),
            logoutUseCase = LogoutUseCase(FakeAuthRepo2(), InMemorySessionRepository())
        )

        assertEquals(profile, vm.uiState.value.profile)
        assertEquals(settingsFlow.value, vm.uiState.value.settings)

        profileFlow.value = profile.copy(status = "Перерыв")
        assertEquals("Перерыв", vm.uiState.value.profile?.status)
    }
}
