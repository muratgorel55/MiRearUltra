package com.mirearultra.pro.core

import java.util.Locale

/**
 * Supported top 10 world languages + Auto system detection:
 * 1. English (en) - Default
 * 2. Mandarin Chinese (zh)
 * 3. Spanish (es)
 * 4. Hindi (hi)
 * 5. Arabic (ar)
 * 6. Bengali (bn)
 * 7. Portuguese (pt)
 * 8. Russian (ru)
 * 9. Japanese (ja)
 * 10. Turkish (tr)
 */
enum class AppLanguage(val code: String, val nativeName: String, val englishName: String) {
    AUTO("auto", "Otomatik (Sistem Dili)", "Auto (System Default)"),
    ENGLISH("en", "English", "English"),
    CHINESE("zh", "中文", "Mandarin Chinese"),
    SPANISH("es", "Español", "Spanish"),
    HINDI("hi", "हिन्दी", "Hindi"),
    ARABIC("ar", "العربية", "Arabic"),
    BENGALI("bn", "বাংলা", "Bengali"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    RUSSIAN("ru", "Русский", "Russian"),
    JAPANESE("ja", "日本語", "Japanese"),
    TURKISH("tr", "Türkçe", "Turkish");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: AUTO
        }
    }
}

object RearI18n {

    fun getEffectiveLanguage(): AppLanguage {
        val configured = RearStateRepository.settings.value.appLanguage
        val chosen = AppLanguage.fromCode(configured)
        if (chosen != AppLanguage.AUTO) return chosen

        val sysLang = Locale.getDefault().language.lowercase()
        return when {
            sysLang.startsWith("zh") -> AppLanguage.CHINESE
            sysLang.startsWith("es") -> AppLanguage.SPANISH
            sysLang.startsWith("hi") -> AppLanguage.HINDI
            sysLang.startsWith("ar") -> AppLanguage.ARABIC
            sysLang.startsWith("bn") -> AppLanguage.BENGALI
            sysLang.startsWith("pt") -> AppLanguage.PORTUGUESE
            sysLang.startsWith("ru") -> AppLanguage.RUSSIAN
            sysLang.startsWith("ja") -> AppLanguage.JAPANESE
            sysLang.startsWith("tr") -> AppLanguage.TURKISH
            else -> AppLanguage.ENGLISH
        }
    }

    fun t(key: I18nKey): String {
        val lang = getEffectiveLanguage()
        return STRINGS[key]?.get(lang) ?: STRINGS[key]?.get(AppLanguage.ENGLISH) ?: key.name
    }

    enum class I18nKey {
        INCOMING_CALL,
        ANSWER,
        DECLINE,
        SPEED_DIAL,
        SPEED_DIAL_SUBTITLE,
        SPEEDOMETER,
        LIVE_SPEED,
        START_MAPS,
        QUICK_TOOLS,
        TORCH,
        VOICE_MEMO,
        SPIRIT_LEVEL,
        REC_START,
        REC_STOP,
        NOTIFICATIONS,
        CLEAR_ALL,
        CLOSE,
        MINI_GAME,
        GAME_OVER,
        RESTART,
        SCORE,
        STEP_COUNT,
        REAR_PREVIEW,
        MODULES,
        PERMISSIONS_PRIVACY,
        CUSTOMIZE,
        SELECT_CONTACTS,
        PICK_FROM_CONTACTS,
        MANUAL_ADD,
        AUTO_FILL_CONTACTS
    }

    private val STRINGS = mapOf(
        I18nKey.INCOMING_CALL to mapOf(
            AppLanguage.ENGLISH to "INCOMING CALL",
            AppLanguage.CHINESE to "来电",
            AppLanguage.SPANISH to "LLAMADA ENTRANTE",
            AppLanguage.HINDI to "आने वाली कॉल",
            AppLanguage.ARABIC to "مكالمة واردة",
            AppLanguage.BENGALI to "আগত কল",
            AppLanguage.PORTUGUESE to "CHAMADA RECEBIDA",
            AppLanguage.RUSSIAN to "ВХОДЯЩИЙ ЗВОНОК",
            AppLanguage.JAPANESE to "着信",
            AppLanguage.TURKISH to "GELEN ARAMA"
        ),
        I18nKey.ANSWER to mapOf(
            AppLanguage.ENGLISH to "Answer",
            AppLanguage.CHINESE to "接听",
            AppLanguage.SPANISH to "Contestar",
            AppLanguage.HINDI to "उत्तर दें",
            AppLanguage.ARABIC to "رد",
            AppLanguage.BENGALI to "উত্তর দিন",
            AppLanguage.PORTUGUESE to "Atender",
            AppLanguage.RUSSIAN to "Ответить",
            AppLanguage.JAPANESE to "応答",
            AppLanguage.TURKISH to "Cevapla"
        ),
        I18nKey.DECLINE to mapOf(
            AppLanguage.ENGLISH to "Decline",
            AppLanguage.CHINESE to "拒绝",
            AppLanguage.SPANISH to "Rechazar",
            AppLanguage.HINDI to "अस्वीकार",
            AppLanguage.ARABIC to "رفض",
            AppLanguage.BENGALI to "প্রত্যাখ্যান",
            AppLanguage.PORTUGUESE to "Recusar",
            AppLanguage.RUSSIAN to "Отклонить",
            AppLanguage.JAPANESE to "拒否",
            AppLanguage.TURKISH to "Reddet"
        ),
        I18nKey.SPEED_DIAL to mapOf(
            AppLanguage.ENGLISH to "SPEED DIAL",
            AppLanguage.CHINESE to "快速拨号",
            AppLanguage.SPANISH to "MARCACIÓN RÁPIDA",
            AppLanguage.HINDI to "स्पीड डायल",
            AppLanguage.ARABIC to "الاتصال السريع",
            AppLanguage.BENGALI to "দ্রুত ডায়াল",
            AppLanguage.PORTUGUESE to "DISCAGEM RÁPIDA",
            AppLanguage.RUSSIAN to "БЫСТРЫЙ НАБОР",
            AppLanguage.JAPANESE to "短縮ダイヤル",
            AppLanguage.TURKISH to "HIZLI ARAMA"
        ),
        I18nKey.SPEED_DIAL_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Top 6 contacts • Tap to call hands-free",
            AppLanguage.CHINESE to "常用 6 位联系人 • 点击免提呼叫",
            AppLanguage.SPANISH to "Top 6 contactos • Toca para llamar manos libres",
            AppLanguage.HINDI to "शीर्ष 6 संपर्क • हैंड्स-फ़्री कॉल करने के लिए टैप करें",
            AppLanguage.ARABIC to "أهم 6 جهات اتصال • انقر للاتصال بدون استخدام اليدين",
            AppLanguage.BENGALI to "শীর্ষ ৬ যোগাযোগ • হ্যান্ডস-ফ্রি কল করতে ট্যাপ করুন",
            AppLanguage.PORTUGUESE to "Top 6 contatos • Toque para ligar viva-voz",
            AppLanguage.RUSSIAN to "Топ 6 контактов • Нажмите для громкой связи",
            AppLanguage.JAPANESE to "よく使う6件 • タップしてスピーカー通話",
            AppLanguage.TURKISH to "Sık aranan 6 kişi • Dokunarak eller serbest ara"
        ),
        I18nKey.SPEEDOMETER to mapOf(
            AppLanguage.ENGLISH to "SPEEDOMETER",
            AppLanguage.CHINESE to "速度计",
            AppLanguage.SPANISH to "VELOCÍMETRO",
            AppLanguage.HINDI to "स्पीडोमीटर",
            AppLanguage.ARABIC to "عداد السرعة",
            AppLanguage.BENGALI to "স্পিডোমিটার",
            AppLanguage.PORTUGUESE to "VELOCÍMETRO",
            AppLanguage.RUSSIAN to "СПИДОМЕТР",
            AppLanguage.JAPANESE to "スピードメーター",
            AppLanguage.TURKISH to "HIZ GÖSTERGESİ"
        ),
        I18nKey.LIVE_SPEED to mapOf(
            AppLanguage.ENGLISH to "Live Speed",
            AppLanguage.CHINESE to "实时车速",
            AppLanguage.SPANISH to "Velocidad en vivo",
            AppLanguage.HINDI to "लाइव गति",
            AppLanguage.ARABIC to "السرعة الحية",
            AppLanguage.BENGALI to "লাইভ গতি",
            AppLanguage.PORTUGUESE to "Velocidade em tempo real",
            AppLanguage.RUSSIAN to "Текущая скорость",
            AppLanguage.JAPANESE to "現在の速度",
            AppLanguage.TURKISH to "Canlı Sürüş Hızı"
        ),
        I18nKey.START_MAPS to mapOf(
            AppLanguage.ENGLISH to "Start Maps",
            AppLanguage.CHINESE to "启动地图",
            AppLanguage.SPANISH to "Iniciar Mapas",
            AppLanguage.HINDI to "मानचित्र शुरू करें",
            AppLanguage.ARABIC to "بدء الخرائط",
            AppLanguage.BENGALI to "মানচিত্র শুরু করুন",
            AppLanguage.PORTUGUESE to "Iniciar Mapas",
            AppLanguage.RUSSIAN to "Открыть карты",
            AppLanguage.JAPANESE to "マップ起動",
            AppLanguage.TURKISH to "Harita Başlat"
        ),
        I18nKey.QUICK_TOOLS to mapOf(
            AppLanguage.ENGLISH to "QUICK TOOLS",
            AppLanguage.CHINESE to "快捷工具",
            AppLanguage.SPANISH to "HERRAMIENTAS",
            AppLanguage.HINDI to "त्वरित उपकरण",
            AppLanguage.ARABIC to "أدوات سريعة",
            AppLanguage.BENGALI to "দ্রুত সরঞ্জাম",
            AppLanguage.PORTUGUESE to "FERRAMENTAS",
            AppLanguage.RUSSIAN to "ИНСТРУМЕНТЫ",
            AppLanguage.JAPANESE to "クイックツール",
            AppLanguage.TURKISH to "HIZLI ARAÇLAR"
        ),
        I18nKey.TORCH to mapOf(
            AppLanguage.ENGLISH to "Flashlight",
            AppLanguage.CHINESE to "手电筒",
            AppLanguage.SPANISH to "Linterna",
            AppLanguage.HINDI to "टॉर्च",
            AppLanguage.ARABIC to "مصباح",
            AppLanguage.BENGALI to "টর্চ",
            AppLanguage.PORTUGUESE to "Lanterna",
            AppLanguage.RUSSIAN to "Фонарик",
            AppLanguage.JAPANESE to "懐中電灯",
            AppLanguage.TURKISH to "El Feneri"
        ),
        I18nKey.VOICE_MEMO to mapOf(
            AppLanguage.ENGLISH to "Voice Memo",
            AppLanguage.CHINESE to "录音备忘",
            AppLanguage.SPANISH to "Nota de voz",
            AppLanguage.HINDI to "वॉयस मेमो",
            AppLanguage.ARABIC to "مذكرة صوتية",
            AppLanguage.BENGALI to "ভয়েস মেমো",
            AppLanguage.PORTUGUESE to "Gravador de voz",
            AppLanguage.RUSSIAN to "Диктофон",
            AppLanguage.JAPANESE to "ボイスメモ",
            AppLanguage.TURKISH to "Sesli Not"
        ),
        I18nKey.SPIRIT_LEVEL to mapOf(
            AppLanguage.ENGLISH to "Spirit Level",
            AppLanguage.CHINESE to "水平仪",
            AppLanguage.SPANISH to "Nivel",
            AppLanguage.HINDI to "स्पिरिट लेवल",
            AppLanguage.ARABIC to "ميزان الماء",
            AppLanguage.BENGALI to "স্পিরিট লেভেল",
            AppLanguage.PORTUGUESE to "Nível de bolha",
            AppLanguage.RUSSIAN to "Уровень",
            AppLanguage.JAPANESE to "水準器",
            AppLanguage.TURKISH to "Su Terazisi"
        ),
        I18nKey.REC_START to mapOf(
            AppLanguage.ENGLISH to "Record",
            AppLanguage.CHINESE to "录音",
            AppLanguage.SPANISH to "Grabar",
            AppLanguage.HINDI to "रिकॉर्ड",
            AppLanguage.ARABIC to "تسجيل",
            AppLanguage.BENGALI to "রেকর্ড",
            AppLanguage.PORTUGUESE to "Gravar",
            AppLanguage.RUSSIAN to "Запись",
            AppLanguage.JAPANESE to "録音",
            AppLanguage.TURKISH to "Kaydet"
        ),
        I18nKey.REC_STOP to mapOf(
            AppLanguage.ENGLISH to "Stop",
            AppLanguage.CHINESE to "停止",
            AppLanguage.SPANISH to "Detener",
            AppLanguage.HINDI to "रोकें",
            AppLanguage.ARABIC to "إيقاف",
            AppLanguage.BENGALI to "থামুন",
            AppLanguage.PORTUGUESE to "Parar",
            AppLanguage.RUSSIAN to "Стоп",
            AppLanguage.JAPANESE to "停止",
            AppLanguage.TURKISH to "Durdur"
        ),
        I18nKey.NOTIFICATIONS to mapOf(
            AppLanguage.ENGLISH to "NOTIFICATIONS",
            AppLanguage.CHINESE to "通知中心",
            AppLanguage.SPANISH to "NOTIFICACIONES",
            AppLanguage.HINDI to "सूचनाएं",
            AppLanguage.ARABIC to "الإشعارات",
            AppLanguage.BENGALI to "বিজ্ঞপ্তি",
            AppLanguage.PORTUGUESE to "NOTIFICAÇÕES",
            AppLanguage.RUSSIAN to "УВЕДОМЛЕНИЯ",
            AppLanguage.JAPANESE to "通知センター",
            AppLanguage.TURKISH to "BİLDİRİM MERKEZİ"
        ),
        I18nKey.CLEAR_ALL to mapOf(
            AppLanguage.ENGLISH to "Clear All",
            AppLanguage.CHINESE to "全部清除",
            AppLanguage.SPANISH to "Borrar todo",
            AppLanguage.HINDI to "सभी साफ़ करें",
            AppLanguage.ARABIC to "مسح الكل",
            AppLanguage.BENGALI to "সব মুছুন",
            AppLanguage.PORTUGUESE to "Limpar tudo",
            AppLanguage.RUSSIAN to "Очистить всё",
            AppLanguage.JAPANESE to "すべて消去",
            AppLanguage.TURKISH to "Tümünü Sil"
        ),
        I18nKey.CLOSE to mapOf(
            AppLanguage.ENGLISH to "Close",
            AppLanguage.CHINESE to "关闭",
            AppLanguage.SPANISH to "Cerrar",
            AppLanguage.HINDI to "बंद करें",
            AppLanguage.ARABIC to "إغلاق",
            AppLanguage.BENGALI to "বন্ধ করুন",
            AppLanguage.PORTUGUESE to "Fechar",
            AppLanguage.RUSSIAN to "Закрыть",
            AppLanguage.JAPANESE to "閉じる",
            AppLanguage.TURKISH to "Kapat"
        ),
        I18nKey.MINI_GAME to mapOf(
            AppLanguage.ENGLISH to "RETRO PONG (120Hz LTPO)",
            AppLanguage.CHINESE to "复古乒乓 (120Hz LTPO)",
            AppLanguage.SPANISH to "RETRO PONG (120Hz LTPO)",
            AppLanguage.HINDI to "रेट्रो पोंग (120Hz LTPO)",
            AppLanguage.ARABIC to "بونغ الكلاسيكية (120Hz LTPO)",
            AppLanguage.BENGALI to "রেট্রো পং (120Hz LTPO)",
            AppLanguage.PORTUGUESE to "RETRO PONG (120Hz LTPO)",
            AppLanguage.RUSSIAN to "РЕТРО ПОНГ (120Hz LTPO)",
            AppLanguage.JAPANESE to "レトロポン (120Hz LTPO)",
            AppLanguage.TURKISH to "RETRO PONG (120Hz LTPO)"
        ),
        I18nKey.GAME_OVER to mapOf(
            AppLanguage.ENGLISH to "GAME OVER",
            AppLanguage.CHINESE to "游戏结束",
            AppLanguage.SPANISH to "JUEGO TERMINADO",
            AppLanguage.HINDI to "खेल समाप्त",
            AppLanguage.ARABIC to "انتهت اللعبة",
            AppLanguage.BENGALI to "খেলা শেষ",
            AppLanguage.PORTUGUESE to "FIM DE JOGO",
            AppLanguage.RUSSIAN to "ИГРА ОКОНЧЕНА",
            AppLanguage.JAPANESE to "ゲームオーバー",
            AppLanguage.TURKISH to "OYUN BİTTİ"
        ),
        I18nKey.RESTART to mapOf(
            AppLanguage.ENGLISH to "Play Again",
            AppLanguage.CHINESE to "再玩一次",
            AppLanguage.SPANISH to "Jugar de nuevo",
            AppLanguage.HINDI to "फिर से खेलें",
            AppLanguage.ARABIC to "العب مجدداً",
            AppLanguage.BENGALI to "আবার খেলুন",
            AppLanguage.PORTUGUESE to "Jogar Novamente",
            AppLanguage.RUSSIAN to "Сыграть снова",
            AppLanguage.JAPANESE to "もう一度",
            AppLanguage.TURKISH to "Yeniden Başlat"
        ),
        I18nKey.SCORE to mapOf(
            AppLanguage.ENGLISH to "SCORE",
            AppLanguage.CHINESE to "得分",
            AppLanguage.SPANISH to "PUNTOS",
            AppLanguage.HINDI to "स्कोर",
            AppLanguage.ARABIC to "النتيجة",
            AppLanguage.BENGALI to "স্কোর",
            AppLanguage.PORTUGUESE to "PONTOS",
            AppLanguage.RUSSIAN to "СЧЁТ",
            AppLanguage.JAPANESE to "スコア",
            AppLanguage.TURKISH to "SKOR"
        ),
        I18nKey.STEP_COUNT to mapOf(
            AppLanguage.ENGLISH to "Steps",
            AppLanguage.CHINESE to "步数",
            AppLanguage.SPANISH to "Pasos",
            AppLanguage.HINDI to "कदम",
            AppLanguage.ARABIC to "خطوة",
            AppLanguage.BENGALI to "পদক্ষেপ",
            AppLanguage.PORTUGUESE to "Passos",
            AppLanguage.RUSSIAN to "Шагов",
            AppLanguage.JAPANESE to "歩数",
            AppLanguage.TURKISH to "Adım"
        ),
        I18nKey.REAR_PREVIEW to mapOf(
            AppLanguage.ENGLISH to "REAR SCREEN LIVE PREVIEW",
            AppLanguage.CHINESE to "副屏实时预览",
            AppLanguage.SPANISH to "VISTA PREVIA EN VIVO",
            AppLanguage.HINDI to "रियर स्क्रीन लाइव पूर्वावलोकन",
            AppLanguage.ARABIC to "معاينة مباشرة للشاشة الخلفية",
            AppLanguage.BENGALI to "রিয়ার স্ক্রিন লাইভ প্রিভিউ",
            AppLanguage.PORTUGUESE to "PRÉVIA DA TELA TRASEIRA",
            AppLanguage.RUSSIAN to "ПРЕВЬЮ ЗАДНЕГО ЭКРАНА",
            AppLanguage.JAPANESE to "背面ディスプレイ ライブプレビュー",
            AppLanguage.TURKISH to "ARKA EKRAN CANLI ÖNİZLEMESİ"
        ),
        I18nKey.MODULES to mapOf(
            AppLanguage.ENGLISH to "Feature Modules",
            AppLanguage.CHINESE to "功能模块",
            AppLanguage.SPANISH to "Módulos",
            AppLanguage.HINDI to "सुविधा मॉड्यूल",
            AppLanguage.ARABIC to "وحدات الميزات",
            AppLanguage.BENGALI to "মডিউল",
            AppLanguage.PORTUGUESE to "Módulos",
            AppLanguage.RUSSIAN to "Модули",
            AppLanguage.JAPANESE to "モジュール",
            AppLanguage.TURKISH to "Modüller"
        ),
        I18nKey.PERMISSIONS_PRIVACY to mapOf(
            AppLanguage.ENGLISH to "Permissions & Privacy",
            AppLanguage.CHINESE to "权限与隐私",
            AppLanguage.SPANISH to "Permisos y Privacidad",
            AppLanguage.HINDI to "अनुमतियाँ और गोपनीयता",
            AppLanguage.ARABIC to "الأذونات والخصوصية",
            AppLanguage.BENGALI to "অনুমতি ও গোপনীয়তা",
            AppLanguage.PORTUGUESE to "Permissões e Privacidade",
            AppLanguage.RUSSIAN to "Разрешения и приватность",
            AppLanguage.JAPANESE to "権限とプライバシー",
            AppLanguage.TURKISH to "İzin & Gizlilik"
        ),
        I18nKey.CUSTOMIZE to mapOf(
            AppLanguage.ENGLISH to "Customize",
            AppLanguage.CHINESE to "个性化定制",
            AppLanguage.SPANISH to "Personalizar",
            AppLanguage.HINDI to "अनुकूलित करें",
            AppLanguage.ARABIC to "تخصيص",
            AppLanguage.BENGALI to "কাস্টমাইজ",
            AppLanguage.PORTUGUESE to "Personalizar",
            AppLanguage.RUSSIAN to "Настройки",
            AppLanguage.JAPANESE to "カスタマイズ",
            AppLanguage.TURKISH to "Özelleştir"
        ),
        I18nKey.SELECT_CONTACTS to mapOf(
            AppLanguage.ENGLISH to "Speed Dial Contacts (6 Contacts)",
            AppLanguage.CHINESE to "快速拨号联系人 (6位)",
            AppLanguage.SPANISH to "Contactos de marcación rápida (6)",
            AppLanguage.HINDI to "स्पीड डायल संपर्क (6 संपर्क)",
            AppLanguage.ARABIC to "جهات اتصال الاتصال السريع (6 جهات)",
            AppLanguage.BENGALI to "স্পিড ডায়াল পরিচিতি (৬ জন)",
            AppLanguage.PORTUGUESE to "Contatos de discagem rápida (6)",
            AppLanguage.RUSSIAN to "Контакты быстрого набора (6)",
            AppLanguage.JAPANESE to "短縮ダイヤル連絡先 (6件)",
            AppLanguage.TURKISH to "Hızlı Aranacak Kişiler (6 Kişi)"
        ),
        I18nKey.PICK_FROM_CONTACTS to mapOf(
            AppLanguage.ENGLISH to "Pick from Contacts",
            AppLanguage.CHINESE to "从通讯录选取",
            AppLanguage.SPANISH to "Elegir de contactos",
            AppLanguage.HINDI to "संपर्कों से चुनें",
            AppLanguage.ARABIC to "اختر من جهات الاتصال",
            AppLanguage.BENGALI to "পরিচিতি থেকে চয়ন করুন",
            AppLanguage.PORTUGUESE to "Escolher dos contatos",
            AppLanguage.RUSSIAN to "Выбрать из контактов",
            AppLanguage.JAPANESE to "連絡先から選択",
            AppLanguage.TURKISH to "Rehberden Seç"
        ),
        I18nKey.MANUAL_ADD to mapOf(
            AppLanguage.ENGLISH to "Manual Add",
            AppLanguage.CHINESE to "手动添加",
            AppLanguage.SPANISH to "Agregar manual",
            AppLanguage.HINDI to "मैन्युअल जोड़ें",
            AppLanguage.ARABIC to "إضافة يدوية",
            AppLanguage.BENGALI to "ম্যানুয়াল যোগ করুন",
            AppLanguage.PORTUGUESE to "Adicionar manual",
            AppLanguage.RUSSIAN to "Добавить вручную",
            AppLanguage.JAPANESE to "手動で追加",
            AppLanguage.TURKISH to "Manuel Ekle"
        ),
        I18nKey.AUTO_FILL_CONTACTS to mapOf(
            AppLanguage.ENGLISH to "Auto-fill Most Called Contacts",
            AppLanguage.CHINESE to "自动填充最常联系人",
            AppLanguage.SPANISH to "Autocompletar más llamados",
            AppLanguage.HINDI to "सबसे अधिक कॉल किए गए संपर्कों को स्वतः भरें",
            AppLanguage.ARABIC to "ملء تلقائي لجهات الاتصال الأكثر اتصالاً",
            AppLanguage.BENGALI to "সর্বাধিক কল করা পরিচিতিগুলি স্বয়ংক্রিয়ভাবে পূরণ করুন",
            AppLanguage.PORTUGUESE to "Preencher mais chamados automaticamente",
            AppLanguage.RUSSIAN to "Автозаполнение частых контактов",
            AppLanguage.JAPANESE to "よく通話する連絡先を自動入力",
            AppLanguage.TURKISH to "En Çok Arananları Otomatik Doldur"
        )
    )
}
