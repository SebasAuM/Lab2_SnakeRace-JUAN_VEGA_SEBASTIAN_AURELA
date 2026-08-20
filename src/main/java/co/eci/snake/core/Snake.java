package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

//El import pal mutex
import java.util.concurrent.locks.ReentrantLock;

public final class Snake {
  
  private final Deque<Position> body = new ArrayDeque<>();
  private volatile Direction direction;
  private int maxLength = 5;

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  public Direction direction() { 
    mutex.lock();
    try {
      return direction;
    } finally {
      mutex.unlock();
    }
   }

  public void turn(Direction dir) {
    mutex.lock();
    try {
      if ((direction == Direction.UP && dir == Direction.DOWN) ||
          (direction == Direction.DOWN && dir == Direction.UP) ||
          (direction == Direction.LEFT && dir == Direction.RIGHT) ||
          (direction == Direction.RIGHT && dir == Direction.LEFT)) {
        return;
      }
      this.direction = dir;
    } finally {
      mutex.unlock();
    }
  }

  // creamos el lock para que no haya problemas de concurrencia al acceder a la cabeza de la serpiente
  private final ReentrantLock mutex = new ReentrantLock();

  // alive / death tracking
  private volatile boolean alive = true;
  private volatile long deathTime = -1;

  public Position head() { 
    //SOLUCION IMPLEMENTACION DEL MUTEX, BLOQUEAMOS EL MUTEX ANTES DE ACCEDER A LA CABEZA DE LA SERPIENTE Y LO DESBLOQUEAMOS DESPUES DE ACCEDER A ELLA
    mutex.lock();
    try {
      return body.peekFirst();
    } finally {
      mutex.unlock();
    }
  }

  public Deque<Position> snapshot() { 
    //SOLUCION IMPLEMENTACION DEL MUTEX, BLOQUEAMOS EL MUTEX ANTES DE ACCEDER A LA COPIA DE LA SERPIENTE Y LO DESBLOQUEAMOS DESPUES DE ACCEDER A ELLA
    mutex.lock();
    try {
      return new ArrayDeque<>(body);
    } finally {
      mutex.unlock();
    }
  }

  public boolean isAlive() {
    return alive;
  }

  public void markDead() {
    if (alive) {
      alive = false;
      deathTime = System.currentTimeMillis();
    }
  }

  public long deathTime() { return deathTime; }

  public void advance(Position newHead, boolean grow) {
    mutex.lock();
    try {
      body.addFirst(newHead);
      if (grow) maxLength++;
      while (body.size() > maxLength) body.removeLast();
    } finally {
      mutex.unlock();
    }
  }
}
