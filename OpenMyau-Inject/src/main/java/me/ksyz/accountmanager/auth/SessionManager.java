package me.ksyz.accountmanager.auth;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class SessionManager {
  private static final Minecraft mc = Minecraft.getMinecraft();
  private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

  private static volatile Field field;

  private static Field getField() {
    if (field == null) {
      synchronized (SessionManager.class) {
        if (field != null) {
          return field;
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
          if (f.getType() == Session.class) {
            try {
              f.setAccessible(true);
            } catch (RuntimeException e) {
              throw new IllegalStateException("Could not access Minecraft session field", e);
            }
            field = f;
            break;
          }
        }
        if (field == null) {
          throw new IllegalStateException("Could not locate Minecraft session field");
        }
      }
    }
    return field;
  }

  public static Session get() {
    return mc.getSession();
  }

  public static Session offline(String username) {
    String uuid =
        UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
    return new Session(username, uuid, "0", Session.Type.LEGACY.toString());
  }

  public static void set(Session session) {
    if (session == null) {
      throw new IllegalArgumentException("session must not be null");
    }
    final Field sessionField = getField();
    Runnable updateSession =
        new Runnable() {
          @Override
          public void run() {
            try {
              sessionField.set(mc, session);
            } catch (IllegalAccessException e) {
              throw new IllegalStateException("Could not update Minecraft session", e);
            }
          }
        };
    if (mc.isCallingFromMinecraftThread()) {
      updateSession.run();
      return;
    }
    try {
      mc.addScheduledTask(updateSession).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while updating Minecraft session", e);
    } catch (ExecutionException e) {
      LOGGER.log(Level.SEVERE, "Could not update Minecraft session", e.getCause());
      throw new IllegalStateException("Could not update Minecraft session", e.getCause());
    }
  }
}
