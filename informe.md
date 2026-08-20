# Laboratorio: Concurrencia y Sincronización en SnakeRace
---
## Parte I - Wait notify exercise 
https://github.com/JDeltax/wait-notify-excercise-AURELAVEGA

Para esta solucion se utiliza un "orquestador de hilos" uno que controle a todos los demás para un avisado de cuando pararlos etc.
Ademas de una variable de estado por hilo para notificar si están o no pausados o no cuando llegan a su momento t milisegundo.   
estos hilos tambien deben estar sincronizados en el control
  - Se pone `paused=true` dentro de un bloque sincronizado para que la transición sea visible y atómica respecto a `awaitIfPaused()`.
  - Después de que el usuario pulse ENTER, se hace `paused=false` y `notifyAll()` dentro del mismo monitor. `notifyAll()` despierta a todos los hilos que están en `wait()` sobre ese monitor.
---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

#### • Autonomía de las serpientes mediante hilos
El diseño del programa encapsula la lógica y estado de una serpiente dentro de su respectiva clase. La autonomía de cada entidad se logra asignando un **hilo independiente (`Thread`) por cada instancia de serpiente**. Cada hilo ejecuta su propio ciclo de vida (movimiento, cambio de dirección, validación de estado y actualización en el tablero), operando de manera asíncrona respecto a las demás serpientes y al hilo principal de la interfaz gráfica (UI).

#### • Posibles condiciones de carrera
* **Acceso y modificación concurrente del tablero/posiciones:** Múltiples hilos intentan consultar y sobreescribir simultáneamente las casillas del tablero compartido (por ejemplo, al verificar colisiones, comer o actualizar la posición de la cabeza).
* **Cambios de dirección y actualización de cuerpo:** Ocurren inconsistencias cuando se capturan eventos de entrada (teclado/IA) para cambiar la dirección mientras el hilo de la serpiente está en pleno proceso de avanzar y redibujar los segmentos de su cuerpo.

#### • Colecciones o estructuras no seguras (`Not Thread-Safe`)
* **`ArrayDeque`:** La estructura de datos utilizada para almacenar los segmentos del cuerpo de cada serpiente no es segura para entornos concurrentes. Si un hilo externo (o el hilo de renderizado/pausa) intenta iterar, leer o copiar la estructura mientras el hilo de la serpiente agrega o remueve nodos (`addFirst`, `removeLast`), se producen inconsistencias de estado o excepciones del tipo `ConcurrentModificationException`.

#### • Ocurrencias de espera activa (*busy-wait*) y sincronización
* Durante la inspección del código base **no se evidenciaron bucles de espera activa (*busy-wait*) explícitos** (bucles `while` vacíos o consumiendo CPU a la espera de un cambio de estado booleano). El control de intervalos se apoya principalmente en pausas temporizadas o eventos de reloj.

---

### 2) Correcciones mínimas y regiones críticas

#### • Identificación del riesgo y solución aplicada
* **Riesgo:** Inconsistencias de memoria, colisiones fantasmas y corrupción del estado interno del cuerpo de las serpientes debido a accesos simultáneos y no sincronizados.
* **Solución:**
  * Se definieron **regiones críticas atómicas y acotadas** protegiendo únicamente las operaciones estrictamente necesarias (modificación de casillas en el tablero y actualización de la cola/cabeza en la colección).
  * Se implementaron mecanismos de exclusión mutua (**Mutex / bloques `synchronized`**) sobre los recursos compartidos, evitando bloqueos de granularidad gruesa que degraden el rendimiento general del juego.
  * Para los estados de pausa/reanudación se emplean mecanismos de notificación y espera (`wait()` / `notifyAll()` o primitivas de `java.util.concurrent`) en lugar de chequeos periódicos en bucle.

---

### 3) Control de ejecución seguro (UI)

#### • Implementación de Iniciar / Pausar / Reanudar
* Se asoció el control de flujo al botón **Action** y al temporizador `GameClock`.
* Al pulsar el botón de pausa:
  1. Se emite una señal de suspensión coordinada a todos los hilos de las serpientes.
  2. Se espera a que cada hilo complete de manera segura su paso actual de simulación (evitando estados intermedios o lecturas incompletas/_tearing_).
  3. Una vez alcanzado el estado estacionario, se extraen y muestran de forma consistente en la UI:
     * **La serpiente viva más larga** (mayor número de segmentos acumulados).
     * **La peor serpiente** (la primera en morir durante la partida, registrada cronológicamente al momento del evento de colisión/muerte).

---

### 4) Robustez bajo carga

* **Pruebas de estrés:** Se ejecutó el simulador configurando un número elevado de serpientes concurrentes (`-Dsnakes=20` o superior) e incrementando la velocidad de actualización del reloj de juego.
* **Resultados observados:**
Dificil manejo pero no situaciones de carrera observadas o alguna excepción que haya saltado.
  * **Cero `ConcurrentModificationException`:** La protección de las colecciones y la atomicidad de las lecturas mitigaron los fallos de modificación concurrente.
  * **Ausencia de Deadlocks:** El orden estricto de adquisición de bloqueos y la reducción del alcance de las secciones críticas garantizaron fluidez sin interbloqueos.
  * **Reglas con Teleports y Turbo:** Las transiciones de teletransporte e incrementos bruscos de velocidad respetan la sincronización sobre el tablero, previniendo carreras en la adjudicación de casillas destino.
