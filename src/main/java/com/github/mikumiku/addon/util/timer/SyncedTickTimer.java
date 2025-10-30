package com.github.mikumiku.addon.util.timer;

public class SyncedTickTimer implements Timer {
   private long time = 0L;

   SyncedTickTimer() {
   }

   @Override
   public void reset() {
      this.time = TickTimerManager.INSTANCE.getTickTime();
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
      if (TickTimerManager.INSTANCE.getTickTime() - this.time > delay) {
         if (resetIfTick) {
            this.time = TickTimerManager.INSTANCE.getTickTime();
         }

         return true;
      } else {
         return false;
      }
   }
}
