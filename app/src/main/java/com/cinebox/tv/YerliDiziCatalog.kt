package com.cinebox.tv

internal val yerliDiziCatalog = listOf(
    Movie(id = 960001, title = "Kurtlar Vadisi", overview = "Kurtlar Vadisi bölüm listesini açar. İzlemek istediğiniz bölümü listeden seçin. Listenin tüm bölümleri içerdiği ve cihazda oynatımı doğrulanmadı.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "YouTube · Bölüm listesi", webUrl = "https://www.youtube.com/playlist?list=PLnJQRSNjIxjvdeIJHnIuVx-6d3aRe_v0X", category = "Yerli Diziler"),
    Movie(id = 960002, title = "Kurtlar Vadisi Pusu", overview = "Kanal D bölüm arşivi. Bu arşiv 230–300. bölümleri listeler; ilk 229 bölüm bu kaynakta yer almıyor. Bölümü açılan sayfadan seçin.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "Kanal D", webUrl = "https://www.kanald.com.tr/kurtlar-vadisi-pusu/bolumler", category = "Yerli Diziler"),
    Movie(id = 960003, title = "Yarım Elma", overview = "Show TV bölüm arşivi. Sayfadaki Bölümler alanından seçin. Arşivde 18. bölüme kadar kayıtlar görülüyor; dizinin tamamı olarak doğrulanmadı. Kanal D 1. bölüm bağlantısı da aşağıda yer alır.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "Show TV", webUrl = "https://www.showtv.com.tr/dizi/tanitim/yarim-elma/1050", category = "Yerli Diziler"),
    Movie(id = 960004, title = "Aşk-ı Memnu", overview = "Kanal D bölüm arşivi. Sezon ve sayfa seçerek bölümlere ulaşabilirsiniz; arşivde 79. bölüm finali de listelenir.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "Kanal D", webUrl = "https://www.kanald.com.tr/askimemnu/bolumler", category = "Yerli Diziler"),
    Movie(id = 960005, title = "Arıza", overview = "Show TV bölüm arşivi. Bölümler alanından seçim yapın. İlk bölüm ve YouTube bölüm listesi için aşağıdaki bağlantıları da kullanabilirsiniz.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "Show TV", webUrl = "https://www.showtv.com.tr/dizi/tanitim/ariza/2745", category = "Yerli Diziler"),
    Movie(id = 960006, title = "Çiçek Taksi", overview = "Çiçek Taksi bölüm listesini açar. Listeden izlemek istediğiniz bölümü seçin. Dizinin bütün yayın dönemlerini eksiksiz kapsadığı doğrulanmadı.", poster = "", backdrop = "", year = "", score = 0.0, genreIds = emptyList(), provider = "YouTube · TRT bölüm videoları", webUrl = "https://www.youtube.com/playlist?list=PLvl2KbSBJH628KPFZG8bR1LJ8LgCIcXod", category = "Yerli Diziler")
)

internal data class SeriesLink(val label: String, val url: String)
internal val seriesExtraLinks: Map<Int, List<SeriesLink>> = mapOf(
    960003 to listOf(SeriesLink("Kanal D · 1. bölümü aç", "https://www.kanald.com.tr/yarimelma/bolumler/1-bolum")),
    960005 to listOf(
        SeriesLink("Show TV · 1. bölümü aç", "https://www.showtv.com.tr/dizi/tum_bolumler/ariza-sezon-1-bolum-1-izle/89515"),
        SeriesLink("YouTube · Bölüm listesini aç", "https://www.youtube.com/playlist?list=PL9wz9gicMpqCbESSGRgv4zOhSkxH7RA1w")
    )
)
