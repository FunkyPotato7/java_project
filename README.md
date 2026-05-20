# RenovaCalc

RenovaCalc - навчальний Java Swing застосунок для швидкого розрахунку орієнтовної вартості ремонту.

## Можливості

- введення параметрів проєкту: площа, кількість кімнат, місто, бюджет, рівень якості;
- вибір типів робіт і додаткових коефіцієнтів;
- автоматичне формування таблиці кошторису;
- індикатор використання бюджету;
- текстовий підсумок та експорт кошторису у `.txt`;
- генерація скриншотів для звіту.

## Запуск

Потрібен JDK 21 або новіший.

```powershell
.\run.ps1
```

Якщо Windows блокує запуск PowerShell-скриптів, скористайтеся:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Або запустіть `run.bat`.

## Збірка JAR

```powershell
.\build.ps1
java -jar .\build\RenovaCalc.jar
```

Також доступна команда `build.bat`.

## Скриншоти

```powershell
.\build.ps1
java -jar .\build\RenovaCalc.jar --screenshot docs\screenshots
```

Після виконання скриншоти будуть у `docs/screenshots`.

## Публікація на GitHub

Репозиторій: `java_project`.

```powershell
git remote add origin https://github.com/FunkyPotato7/java_project.git
git branch -M main
git push -u origin main
```
