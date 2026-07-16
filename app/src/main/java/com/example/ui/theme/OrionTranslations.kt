package com.example.ui.theme

object OrionTranslations {
    private val en = mapOf(
        "settings_title" to "Orion AI Settings",
        "system_control" to "SYSTEM CONTROL CENTER",
        "customize_desc" to "Customize your companion interface and engine configurations.",
        "sub_management" to "SUBSCRIPTION MANAGEMENT",
        "current_tier" to "Current Tier",
        "upgrade_btn" to "UPGRADE TO ORION PREMIUM",
        "manage_sub" to "MANAGE SUBSCRIPTION",
        "model_title" to "ORION AI COMPANION MODEL",
        "temp_creativity" to "TEMPERATURE / CREATIVITY",
        "firebase_sync" to "FIREBASE CLOUD SYNCHRONIZATION",
        "manual_sync" to "FORCE MANUAL CLOUD SYNC",
        "diagnostics" to "DIAGNOSTICS & TELEMETRY",
        
        // Theme Customization
        "theme_customization" to "THEME CUSTOMIZATION",
        "select_theme_desc" to "Change the aesthetic layout and constellation accent colors.",
        "space_dark" to "Space Dark (Default)",
        "nebula_teal" to "Nebula Teal",
        "aurora_green" to "Aurora Green",
        "light_starlight" to "Light Starlight",
        
        // Multi-language
        "language_support" to "LANGUAGE SELECTION",
        "language_desc" to "Configure your preferred language interface.",
        "lang_en" to "English (EN)",
        "lang_hi" to "Hindi (हिन्दी)",
        
        // Backup & Restore
        "backup_restore" to "DATA BACKUP & PORTABILITY",
        "backup_desc" to "Export your chat history safely or import a previous backup file.",
        "export_backup" to "EXPORT CHATS BACKUP (JSON)",
        "import_backup" to "IMPORT CHATS BACKUP (JSON)",
        
        // System Information and Policies
        "legal_policies" to "LEGAL & ABOUT SECTIONS",
        "privacy_policy" to "Privacy Policy",
        "terms_conditions" to "Terms & Conditions",
        "about_orion" to "About Orion AI",
        "check_updates" to "CHECK FOR SYSTEM UPDATES",
        "update_btn" to "Check for Updates",
        "no_update_toast" to "Your Orion client is fully synchronized and up-to-date!",
        
        // Common Labels
        "back" to "Back",
        "pro_active" to "PRO ACTIVE",
        "basic_tier" to "BASIC TIER",
        "free_desc" to "Access standard chat models. Interstitial and Banner advertisements active.",
        "premium_desc" to "Zero ads. Access analytical Gemini Pro models. High-speed server allocation."
    )

    private val hi = mapOf(
        "settings_title" to "ओरियन एआई सेटिंग्स",
        "system_control" to "सिस्टम नियंत्रण केंद्र",
        "customize_desc" to "अपने साथी इंटरफ़ेस और इंजन कॉन्फ़िगरेशन को अनुकूलित करें।",
        "sub_management" to "सदस्यता प्रबंधन",
        "current_tier" to "वर्तमान स्तर",
        "upgrade_btn" to "ओरियन प्रीमियम में अपग्रेड करें",
        "manage_sub" to "सदस्यता प्रबंधित करें",
        "model_title" to "ओरियन एआई साथी मॉडल",
        "temp_creativity" to "तापमान / रचनात्मकता",
        "firebase_sync" to "फायरबेस क्लाउड सिंक्रनाइज़ेशन",
        "manual_sync" to "मैन्युअल क्लाउड सिंक करें",
        "diagnostics" to "निदान और टेलीमेट्री",
        
        // Theme Customization
        "theme_customization" to "थीम अनुकूलन",
        "select_theme_desc" to "सौंदर्य लेआउट और तारामंडल उच्चारण रंग बदलें।",
        "space_dark" to "स्पेस डार्क (डिफ़ॉल्ट)",
        "nebula_teal" to "नेबुला टील",
        "aurora_green" to "अरोड़ा ग्रीन",
        "light_starlight" to "लाइट स्टारलाइट",
        
        // Multi-language
        "language_support" to "भाषा चयन",
        "language_desc" to "अपनी पसंदीदा भाषा इंटरफ़ेस कॉन्फ़िगर करें।",
        "lang_en" to "अंग्रेजी (EN)",
        "lang_hi" to "हिंदी (हिन्दी)",
        
        // Backup & Restore
        "backup_restore" to "डेटा बैकअप और पोर्टेबिलिटी",
        "backup_desc" to "अपना चैट इतिहास सुरक्षित रूप से निर्यात करें या पिछला बैकअप आयात करें।",
        "export_backup" to "चैट बैकअप निर्यात करें (JSON)",
        "import_backup" to "चैट बैकअप आयात करें (JSON)",
        
        // System Information and Policies
        "legal_policies" to "कानूनी और परिचय अनुभाग",
        "privacy_policy" to "गोपनीयता नीति",
        "terms_conditions" to "नियम और शर्तें",
        "about_orion" to "ओरियन एआई के बारे में",
        "check_updates" to "सिस्टम अपडेट की जांच करें",
        "update_btn" to "अपडेट जांचें",
        "no_update_toast" to "आपका ओरियन क्लाइंट पूरी तरह से सिंक्रनाइज़ और अप-टू-डेट है!",
        
        // Common Labels
        "back" to "पीछे",
        "pro_active" to "प्रो सक्रिय",
        "basic_tier" to "बेसिक टियर",
        "free_desc" to "मानक चैट मॉडल तक पहुंचें। अंतरालीय और बैनर विज्ञापन सक्रिय।",
        "premium_desc" to "शून्य विज्ञापन। विश्लेषणात्मक जेमिनी प्रो मॉडल तक पहुंचें। उच्च गति सर्वर आवंटन।"
    )

    fun getString(key: String, lang: String): String {
        return if (lang == "hi") {
            hi[key] ?: en[key] ?: key
        } else {
            en[key] ?: key
        }
    }
}
