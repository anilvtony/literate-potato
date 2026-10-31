package com.policyregistration.app.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object RegistrationForm : Screen("registration_form")
    object CameraScan : Screen("camera_scan")
    object OcrReview : Screen("ocr_review")
    object RecordsList : Screen("records_list")
    object ExportExcel : Screen("export_excel")
    object CampaignSettings : Screen("campaign_settings")
    object BackupRestore : Screen("backup_restore")
}
