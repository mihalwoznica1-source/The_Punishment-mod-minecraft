# The Punishment — Mod do Minecrafta (Forge 1.20.1)

## Opis
Mod horrorowy dodający encję **"The Punishment"** — nadprzyrodzony byt polujący na graczy podczas **Witching Hour**.

---

## Struktura projektu

```
src/main/java/com/punishment/mod/
├── ThePunishmentMod.java              ← Główna klasa modu
├── entity/
│   ├── ModEntities.java               ← Rejestracja EntityType
│   └── ThePunishmentEntity.java       ← Logika encji (AI, atak, strefy bezpieczeństwa)
├── events/
│   ├── WitchingHourHandler.java       ← Witching Hour: licznik nocy, odliczanie, spawn
│   ├── WitchingHourPacketHelper.java  ← Sieć: pakiety serwer → klient
│   └── ModEventBusSubscriber.java     ← Rejestracja atrybutów encji
├── model/
│   └── ThePunishmentModel.java        ← Model 3D (humanoid)
└── client/
    ├── ClientSetup.java               ← Rejestracja rendererów
    ├── ClientEventBusSubscriber.java  ← ModelLayer registration
    ├── ThePunishmentRenderer.java     ← Renderer (czarna tekstura, brak cienia)
    └── WitchingHourClientHandler.java ← HUD: odliczanie, czerwony overlay, napis RUN

src/main/resources/
├── META-INF/mods.toml
├── pack.mcmeta
└── assets/punishment/
    ├── lang/en_us.json
    └── textures/entity/the_punishment.png  ← Czarny PNG 64x64
```

---

## Mechanika gry

### Witching Hour
| Noc | Szansa aktywacji |
|-----|-----------------|
| 1–4 | 25% |
| 5, 10, 15... | **100% (gwarantowana)** |

Aktywacja następuje dokładnie o **północy** (`dayTime = 18000`).

### Fazy zdarzenia
```
18000 ticks (północ)
    │
    ├── START: "It is 600 blocks away from you"
    │
    ├── Odliczanie 60 sekund (1200 ticków)
    │   └── Aktualizacja wyświetlanej odległości co sekundę
    │
    ├── Zostało 10 sekund (200 ticków):
    │   ├── Czerwony overlay ekranu (pulsujący)
    │   └── Napis "RUN" (3x, czerwony, pulsujący)
    │
    └── 0 ticków: The Punishment teleportuje się do gracza → ATAK
```

### Strefy bezpieczeństwa
| Strefa | Warunek | Efekt |
|--------|---------|-------|
| **Bedrock** | Y ≤ `minBuildHeight + 5` | Gracz przeżywa, encja znika |
| **Strop świata** | Y ≥ `maxBuildHeight - 20` | Gracz przeżywa, encja znika |
| Gdzie indziej | — | **Natychmiastowa śmierć** |

---

## Instalacja i uruchomienie

### Wymagania
- **Java 17+**
- **Forge 47.1.0** (dla Minecraft 1.20.1)
- Gradle (automatycznie przez wrapper)

### Kroki
```bash
# 1. Sklonuj / rozpakuj projekt
cd thepunishment

# 2. Pobierz Forge MDK i skopiuj gradlew, gradle/ do folderu projektu
#    (lub użyj istniejącego wrappera)

# 3. Skonfiguruj środowisko Forge
./gradlew genEclipseRuns   # lub genIntellijRuns

# 4. Uruchom klienta deweloperskiego
./gradlew runClient

# 5. Zbuduj JAR
./gradlew build
# Wynik: build/libs/the-punishment-1.0.0.jar
```

### Testowanie
Po uruchomieniu klienta:
1. Wejdź do świata (tryb przeżycia lub kreatywny)
2. Ustaw czas na północ: `/time set 18000`
3. Poczekaj kilka ticków — sprawdź konsolę na `[ThePunishment]` logi

---

## Konfiguracja (opcjonalna)

Stałe do modyfikacji w `WitchingHourHandler.java`:
```java
BASE_ACTIVATION_CHANCE = 0.25;          // 25% szans bazowych
GUARANTEED_ACTIVATION_EVERY_N_NIGHTS = 5; // co 5. noc = gwarantowana
COUNTDOWN_TICKS = 1200;                 // 60 sekund odliczania
RUN_WARNING_TICKS = 200;                // ostrzeżenie 10s przed atakiem
```

---

## Znane ograniczenia i uwagi

1. **Sieć**: Pakiety (`WitchingHourPacketHelper`) wymagają wywołania `registerPackets()` w `commonSetup`. Dodaj do `ThePunishmentMod.commonSetup()`:
   ```java
   WitchingHourPacketHelper.registerPackets();
   ```

2. **Multiplayer**: Mechanika działa per-gracz — każdy gracz ma własne odliczanie.

3. **Persistent Data**: Liczniki nocy nie są zapisywane do NBT świata — resetują się przy restarcie serwera. Dla persistent storage dodaj zapisy do `ServerSavedData`.

4. **Tekstura**: Plik `the_punishment.png` to czysty czarny PNG wygenerowany skryptem `generate_texture.py`.
