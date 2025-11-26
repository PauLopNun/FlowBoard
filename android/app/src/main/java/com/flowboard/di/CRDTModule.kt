package com.flowboard.di

import com.flowboard.data.crdt.CRDTEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module for providing CRDT-related dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object CRDTModule {

    @Provides
    @ViewModelScoped
    fun provideCRDTEngine(): CRDTEngine {
        return CRDTEngine()
    }
}
