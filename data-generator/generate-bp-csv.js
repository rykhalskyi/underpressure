#!/usr/bin/env node

const args = process.argv.slice(2);

function parseArg(key, defaultValue) {
  const flag = args.find(a => a.startsWith(`--${key}=`));
  return flag ? flag.split('=')[1] : defaultValue;
}

const ROWS = parseInt(parseArg('rows', '30'), 10);
const ANYTIME_PCT = parseInt(parseArg('anytime-pct', '0'), 10);
const ANYTIME_MAX_PER_DAY = parseInt(parseArg('anytime-max-per-day', '3'), 10);
const START_DATE = parseArg('start-date', new Date().toISOString().slice(0, 10));
const DISTRIBUTION_RAW = parseArg('distribution', 'HYPOTENSION:10,NORMAL:25,ELEVATED:20,STAGE_1:20,STAGE_2:15,CRISIS:10');

const LEVELS = ['HYPOTENSION', 'NORMAL', 'ELEVATED', 'STAGE_1', 'STAGE_2', 'CRISIS'];

const distMap = {};
DISTRIBUTION_RAW.split(',').forEach(pair => {
  const [level, pct] = pair.split(':');
  distMap[level] = parseFloat(pct);
});

const distribution = LEVELS.map(l => distMap[l] || 0);
const totalPct = distribution.reduce((a, b) => a + b, 0);
const distributionNorm = distribution.map(d => d / totalPct);

function randomInRange(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function weightedPulse() {
  const r = Math.random();
  if (r < 0.6) return randomInRange(60, 80);
  if (r < 0.85) return randomInRange(55, 59);
  return randomInRange(81, 105);
}

const BP_RANGES = {
  HYPOTENSION: { sys: [70, 89], dia: [40, 65] },
  NORMAL: { sys: [95, 125], dia: [65, 82] },
  ELEVATED: { sys: [130, 139], dia: [85, 89] },
  STAGE_1: { sys: [140, 155], dia: [90, 98] },
  STAGE_2: { sys: [160, 178], dia: [100, 108] },
  CRISIS: { sys: [180, 210], dia: [110, 130] }
};

function validateByEsc(systolic, diastolic, level) {
  if (level === 'HYPOTENSION') return systolic < 90 || diastolic < 60;
  if (level === 'NORMAL') return systolic >= 90 && systolic <= 129 && diastolic >= 60 && diastolic <= 84;
  if (level === 'ELEVATED') return (systolic >= 130 || diastolic >= 85) && systolic < 140 && diastolic < 90;
  if (level === 'STAGE_1') return (systolic >= 140 || diastolic >= 90) && systolic < 160 && diastolic < 100;
  if (level === 'STAGE_2') return (systolic >= 160 || diastolic >= 100) && systolic < 180 && diastolic < 110;
  if (level === 'CRISIS') return systolic >= 180 || diastolic >= 110;
  return false;
}

function clamp(val, min, max) {
  return Math.max(min, Math.min(max, val));
}

function generateReading(level) {
  const range = BP_RANGES[level];
  let sys = randomInRange(range.sys[0], range.sys[1]);
  let dia = randomInRange(range.dia[0], range.dia[1]);
  let attempts = 0;
  while (!validateByEsc(sys, dia, level) && attempts < 20) {
    sys = randomInRange(range.sys[0], range.sys[1]);
    dia = randomInRange(range.dia[0], range.dia[1]);
    attempts++;
  }
  const pulse = weightedPulse();
  return { systolic: sys, diastolic: dia, pulse };
}

function pickLevel() {
  const r = Math.random();
  let cumulative = 0;
  for (let i = 0; i < LEVELS.length; i++) {
    cumulative += distributionNorm[i];
    if (r < cumulative) return LEVELS[i];
  }
  return LEVELS[LEVELS.length - 1];
}

function formatDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatTime(hours, minutes) {
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

function formatReading(r) {
  return `${r.systolic}/${r.diastolic}@${r.pulse}`;
}

function formatAnytimeReading(r, timeStr) {
  return `${r.systolic}/${r.diastolic}@${r.pulse} (${timeStr})`;
}

const totalReadings = ROWS + Math.round(ROWS * ANYTIME_PCT / (100 - ANYTIME_PCT));
const anytimeCount = Math.max(0, totalReadings - ROWS);

let anytimeDist = [];
if (anytimeCount > 0) {
  const basePerRow = Math.floor(anytimeCount / ROWS);
  const remainder = anytimeCount % ROWS;
  for (let i = 0; i < ROWS; i++) {
    let count = basePerRow + (i < remainder ? 1 : 0);
    count = Math.min(count, ANYTIME_MAX_PER_DAY);
    anytimeDist.push(count);
  }
  const currentSum = anytimeDist.reduce((a, b) => a + b, 0);
  let deficit = anytimeCount - currentSum;
  let idx = 0;
  while (deficit > 0) {
    if (anytimeDist[idx % ROWS] < ANYTIME_MAX_PER_DAY) {
      anytimeDist[idx % ROWS]++;
      deficit--;
    }
    idx++;
  }
  anytimeDist = anytimeDist.sort(() => Math.random() - 0.5);
}

const startDate = new Date(START_DATE + 'T00:00:00');

process.stdout.write('Date,Slot 1,Anytime\n');

for (let i = 0; i < ROWS; i++) {
  const date = new Date(startDate);
  date.setDate(startDate.getDate() - i);
  const dateStr = formatDate(date);

  const slot1Level = pickLevel();
  const slot1Reading = generateReading(slot1Level);
  const slot1Str = formatReading(slot1Reading);

  const count = anytimeDist.length > 0 ? anytimeDist[i] : 0;
  let anytimeStr = '\u2014';
  if (count > 0) {
    const readings = [];
    const usedTimes = new Set();
    for (let j = 0; j < count; j++) {
      const level = pickLevel();
      const reading = generateReading(level);
      let hours, minutes, timeStr;
      do {
        hours = randomInRange(0, 23);
        minutes = randomInRange(0, 59);
        timeStr = formatTime(hours, minutes);
      } while (usedTimes.has(timeStr));
      usedTimes.add(timeStr);
      readings.push(formatAnytimeReading(reading, timeStr));
    }
    anytimeStr = readings.join('; ');
  }

  process.stdout.write(`${dateStr},${slot1Str},${anytimeStr}\n`);
}
