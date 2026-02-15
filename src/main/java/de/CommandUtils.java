package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CommandUtils {

    public static String readFileSafe(String path) {
        try {
            return Files.readString(Paths.get(path)).trim();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static String runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command.split(" "));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(line);
            }
            process.waitFor();
            return sb.length() > 0 ? sb.toString() : "Unavailable";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static String runCommandMultiline(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command.split(" "));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            process.waitFor();
            return sb.length() > 0 ? sb.toString() : "Unavailable";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public static void sendMultiline(CommandContext context, String text) {
        if (text == null || text.equals("Unavailable")) {
            context.sendMessage(Message.raw("  Unavailable"));
            return;
        }
        for (String line : text.split("\n")) {
            context.sendMessage(Message.raw("  " + line));
        }
    }
}
