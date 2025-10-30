package com.github.mikumiku.addon.util.timer;

public class StandardTimer implements Timer {
   private long time = System.currentTimeMillis();
   private final long tickTimeMs;

   StandardTimer() {
      this.tickTimeMs = 1L;
   }

   StandardTimer(long tickTimeMs) {
      this.tickTimeMs = tickTimeMs;
   }

   @Override
   public void reset() {
      this.time = System.currentTimeMillis();
   }

   @Override
   public void skip() {
      this.time = 0L;
   }

   @Override
   public boolean tick(long delay) {
      return this.tick(delay, true);
   }

   @Override
   public boolean tick(long delay, boolean resetIfTick) {
      if (System.currentTimeMillis() - this.time > delay * this.tickTimeMs) {
         if (resetIfTick) {
            this.time = System.currentTimeMillis();
         }

         return true;
      } else {
         return false;
      }
   }
}
