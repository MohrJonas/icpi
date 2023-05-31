package mohr.jonas.icpi.cli;

import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.fusesource.jansi.Ansi;

import java.util.Scanner;

import static org.fusesource.jansi.Ansi.ansi;

public class Terminal {

    private final Scanner scanner = new Scanner(System.in);

    public boolean askForBoolean(String question, String trueChar, String falseChar) {
        System.out.println(ansi().fgBrightCyan().a(question).reset());
        while (true) {
            System.out.print(ansi().fg(Ansi.Color.GREEN).a(">> ").reset());
            val input = scanner.nextLine().trim();
            if (input.equals(trueChar))
                return true;
            if (input.equals(falseChar))
                return false;
        }
    }

    public void askForConfirmation(String question, String answer) {
        System.out.println(ansi().fgBrightCyan().a(question).reset());
        while (true) {
            System.out.print(ansi().fg(Ansi.Color.GREEN).a(">> ").reset());
            val input = scanner.nextLine().trim();
            if (input.equals(answer))
                return;
        }
    }

    public int askForNumber(String question, int min, int max) {
        System.out.println(ansi().fgBrightCyan().a(question).reset());
        while (true) {
            System.out.print(ansi().fgBrightCyan().a(">> ").reset());
            val input = scanner.nextLine().trim();
            if (NumberUtils.isParsable(input) && Integer.parseInt(input) >= min && Integer.parseInt(input) <= max)
                return Integer.parseInt(input);
        }
    }

    public void error(String msg, Object... args) {
        System.out.println(ansi().bg(Ansi.Color.RED).a("Error").reset().fg(Ansi.Color.RED).a(" ").a(String.format(msg, args)).reset());
    }

    public void warning(String msg, Object... args) {
        System.out.println(ansi().bg(Ansi.Color.YELLOW).a("Warning").reset().fg(Ansi.Color.YELLOW).a(" ").a(String.format(msg, args)).reset());
    }

    public void success(String msg, Object... args) {
        System.out.println(ansi().bg(Ansi.Color.GREEN).a("Success").reset().fg(Ansi.Color.GREEN).a(" ").a(String.format(msg, args)).reset());
    }

}
