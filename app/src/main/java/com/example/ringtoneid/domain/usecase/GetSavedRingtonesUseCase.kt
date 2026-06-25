package com.example.ringtoneid.domain.usecase

import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedRingtonesUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) {
    operator fun invoke(): Flow<List<RingtoneProfile>> = ringtoneRepository.getSavedRingtones()
}
