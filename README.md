# Zyven FNF Assistant

Sürüm 0.6.1 — FNF ekranını yakalar, dört şeritteki notaları algılar, ekranda gösterir ve istenirse şeritlere otomatik basar.

## Nasıl çalışır
- **Ekran yakalama:** MediaProjection (`CaptureService`), analiz ayrı bir iş parçacığında çalışır.
- **Nota algılama:** `NoteDetector` doygun renkli nota şekillerini bulur ve dört oyuncu şeridine eşler.
- **Zamanlama:** `TapPlanner` her notayı kareden kareye takip eder, hızını ölçer ve vuruş çizgisine varacağı anı hesaplar.
- **Otomatik basma:** `AutoTapService` (AccessibilityService, yalnızca `dispatchGesture`). Pencere içeriğini okumaz, olayları dinlemez.
- **Kontrol balonu:** aç/kapat, vuruş çizgisi, erken basma süresi, rehber katmanı ve test dokunuşu.

## Güvenlik
- Otomatik basma her açılışta **kapalı** başlar; yalnızca balon menüsünden açılır.
- 20 saniye nota görünmezse kendiliğinden kapanır; balon, ekran yakalama veya erişilebilirlik servisi kapanınca da kapanır.

## İlk kurulum
1. Yüzen pencere iznini ver, ekran yakalamayı başlat, kontrol balonunu aç.
2. Ayarlar → Erişilebilirlik → İndirilen uygulamalar → Zyven FNF otomatik dokunma. Engellenirse: Uygulama bilgisi → sağ üst üç nokta → "Kısıtlı ayarlara izin ver".
3. Oyunu yatay aç, balon menüsünde rehber çizgisini ve halkaları oyundaki alıcı oklara hizala (Çizgi ▲▼).
4. "Test dokunuşu" ile dört şeride sırayla dokunulduğunu kontrol et, sonra "Otomatik basma"yı aç.

## Sınırlamalar
- Uzun (basılı tutulan) notalarda yalnızca başına kısa dokunuş yapılır, basılı tutma yoktur.
- Renk/şekil tabanlı deneysel algılama; karakter çizimleri ve efektler yanlış algılama üretebilir.
- Şerit konumları 1600x720 bir kayda göre kalibre edilmiştir.
- Vuruş çizgisi ekranın %5–95'i arasında ayarlanır (▲▲/▼▼ büyük adım, ▲/▼ küçük adım). Aşağı kaydırma (downscroll) için çizgiyi alta indirmek denenebilir; planlayıcı yönden bağımsızdır ama telefonda denenmedi.
- Gecikme (yakalama + dokunuş) cihaza bağlıdır; "Erken" ayarıyla telafi edilir.
