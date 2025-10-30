package com.github.mikumiku.addon.util.timer;

public interface Timer {
   void reset();

   void skip();

   boolean tick(long var1);

   boolean tick(long var1, boolean var3);
}
