# BP CSV Data Generator

Generates random blood pressure readings in CSV format compatible with Underpressure's import. Uses ESC/ESH classification thresholds matching `BloodPressureLevel.kt`.

## Usage

```bash
node generate-bp-csv.js [options] > readings.csv
```

## Options

| Flag | Default | Description |
|------|---------|-------------|
| `--rows=N` | 30 | Number of days |
| `--anytime-pct=N` | 0 | % of total readings that are Anytime (vs Slot 1) |
| `--anytime-max-per-day=M` | 3 | Max Anytime readings on a single day |
| `--start-date=YYYY-MM-DD` | today | First date; subsequent rows increment by 1 day |

## Distribution

Default distribution (testing-oriented):

| Level | % | Systolic | Diastolic |
|-------|---|----------|-----------|
| HYPOTENSION | 10% | 70–89 | 40–65 |
| NORMAL | 25% | 95–125 | 65–82 |
| ELEVATED | 20% | 130–139 | 85–89 |
| STAGE_1 | 20% | 140–155 | 90–98 |
| STAGE_2 | 15% | 160–178 | 100–108 |
| CRISIS | 10% | 180–210 | 110–130 |

Override with `--distribution`:

```bash
node generate-bp-csv.js --distribution=NORMAL:50,ELEVATED:30,STAGE_1:20
```

Unspecified levels get 0%. Values are normalized to sum to 100%.

## Examples

**30 days, Slot 1 only:**
```bash
node generate-bp-csv.js > readings.csv
```

**90 days, 20% Anytime (up to 2 per day):**
```bash
node generate-bp-csv.js --rows=90 --anytime-pct=20 --anytime-max-per-day=2 > readings.csv
```

**Start from a specific date:**
```bash
node generate-bp-csv.js --rows=60 --start-date=2026-01-01 > h1-2026.csv
```

## Output format

```
Date,Slot 1,Anytime
2026-05-16,125/82@72,—
2026-05-17,150/95@85,130/85@75 (14:30)
2026-05-18,138/87@80,132/90@100 (22:46); 140/95@88 (07:15)
```

- `Slot 1` — always populated as `sys/dia@pulse`
- `Anytime` — `—` (em dash) when empty; `sys/dia@pulse (HH:MM)[; ...]` for multiple
- Anytime readings require `(HH:MM)` timestamps — the app's import skips them otherwise
- Pulse is generated weighted toward normal range (60–80 bpm)
