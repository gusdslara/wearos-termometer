# Termômetro Wear OS 🌡️

Aplicativo nativo de **Termômetro** projetado especialmente para smartwatches Wear OS (Galaxy Watch 5, 6, 7, Ultra e compatíveis).

---

## 🎯 Características & Modos

O aplicativo conta com **dois modos de medição de temperatura**:

### 1. Modo no Pulso (Contato com a pele)
- Utiliza os sensores biométricos de contato e calor dérmico integrados no relógio (`Samsung Skin Temp Sensor` / `android.sensor.skin_temp`).
- Mede o contato físico corporal e a dissipação dérmica contínua em repouso.
- Detecta automaticamente quando o relógio está fora do pulso (`Off-body detector`).

### 2. Modo Testa à Distância (Escaneamento IR 1-3 cm)
- Inspirado na operação dos termômetros infravermelhos digitais (estilo ThermoCheck).
- Instruções na tela para apontar o sensor óptico traseiro diretamente ao centro da testa a uma distância de 1 a 3 cm.
- Algoritmo de compensação de temperatura que analisa o diferencial térmico interno/ambiente e calcula a temperatura central do corpo humano.

---

## 🎨 Interface & UX Wear OS

- **Display Circular AMOLED Otimizado:** Fundo 100% preto (`#000000`) para economia máxima de bateria e contraste nítido.
- **Anel de Medição:** Visualização circular do termômetro com indicador de graus Celsius (`°C`).
- **Classificação Visual da Temperatura:**
  - 🔵 **BAIXA** (< 35.5 °C)
  - 🟢 **NORMAL** (35.5 °C – 37.3 °C)
  - 🟡 **FEBRIL** (37.4 °C – 37.8 °C)
  - 🔴 **FEBRE ALTA** (> 37.8 °C)
- **Feedback Háptico (Vibração):** Respostas de vibração ao alternar abas, durante o escaneamento e ao concluir a leitura.
- **Ícone Dedicado:** Ícone circular estilizado de termômetro de mercúrio com gradiente e marcadores de escala.

---

## 🚀 Como Compilar e Instalar

No terminal (PowerShell / Prompt de comando) dentro da pasta `termometro wearOS`:

```bat
# Compilar e instalar passando a porta do Wireless Debugging do seu relógio (ex: 42077):
.\build_install.bat 42077
```

Ou diretamente pelo ADB:
```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
