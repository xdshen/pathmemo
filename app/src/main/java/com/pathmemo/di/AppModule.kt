package com.pathmemo.di

import android.app.Application
import androidx.room.Room
import com.pathmemo.data.db.MIGRATION_1_2
import com.pathmemo.data.db.MIGRATION_2_3
import com.pathmemo.data.db.PathMemoDatabase
import com.pathmemo.data.repository.TrackRepository
import com.pathmemo.data.store.SettingsDataStore
import com.pathmemo.location.CellInfoProvider
import com.pathmemo.location.LocationRecorder
import com.pathmemo.service.LocationRecordService
import com.pathmemo.viewmodel.DayPreviewViewModel
import com.pathmemo.viewmodel.HistoryViewModel
import com.pathmemo.viewmodel.HomeViewModel
import com.pathmemo.viewmodel.OverviewViewModel
import com.pathmemo.viewmodel.RangePreviewViewModel
import com.pathmemo.viewmodel.SettingsViewModel
import com.pathmemo.viewmodel.TrackDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PathMemoDatabase::class.java,
            "pathmemo_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
    single { get<PathMemoDatabase>().trackDao() }
    single { get<PathMemoDatabase>().locationPointDao() }
    single { TrackRepository(get(), get()) }

    single { SettingsDataStore(androidContext()) }
    single { LocationRecorder(androidContext()) }
    single { CellInfoProvider(androidContext()) }

    single { LocationRecordService.BinderManager() }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { OverviewViewModel(get()) }
    viewModel { DayPreviewViewModel(get(), get()) }
    viewModel { RangePreviewViewModel(get(), get()) }
    viewModel { (trackId: Long) -> TrackDetailViewModel(trackId, get(), get()) }
    viewModel { SettingsViewModel(get(), get(), androidContext() as Application) }
}
