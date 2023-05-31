package mohr.jonas.icpi.cli;

import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.util.ArrayList;
import java.util.List;

public class TerminalUtils {

	public boolean askForBoolean(String question, String trueChar, String falseChar, Terminal terminal) {
		val reader = LineReaderBuilder.builder().terminal(terminal).build();
		while (true) {
			val input = reader.readLine(coloredString(terminal, AttributedStyle.CYAN, question));
			if (input.equals(trueChar)) return true;
			if (input.equals(falseChar)) return false;
		}
	}

	public void askForConfirmation(String question, String answer, Terminal terminal) {
		val reader = LineReaderBuilder.builder().terminal(terminal).build();
		while (true) {
			val input = reader.readLine(coloredString(terminal, AttributedStyle.CYAN, question));
			if (input.equals(answer)) return;
		}
	}

	public int askForNumber(String question, int min, int max, Terminal terminal) {
		val reader = LineReaderBuilder.builder().terminal(terminal).build();
		while (true) {
			val input = reader.readLine(coloredString(terminal, AttributedStyle.CYAN, question));
			if (NumberUtils.isParsable(input) && Integer.parseInt(input) >= min && Integer.parseInt(input) <= max)
				return Integer.parseInt(input);
		}
	}

	public void error(Terminal terminal, String msg, Object... args) {
		terminal.writer().println(new AttributedStringBuilder().style(AttributedStyle.DEFAULT.background(AttributedStyle.RED)).append("Error").style(AttributedStyle.DEFAULT).append(" ").style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).append(String.format(msg, args)).toAnsi(terminal));
		terminal.flush();
	}

	public void warning(Terminal terminal, String msg, Object... args) {
		terminal.writer().println(new AttributedStringBuilder().style(AttributedStyle.DEFAULT.background(AttributedStyle.YELLOW)).append("Warning").style(AttributedStyle.DEFAULT).append(" ").style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)).append(String.format(msg, args)).toAnsi(terminal));
		terminal.flush();
	}

	public void success(Terminal terminal, String msg, Object... args) {
		terminal.writer().println(new AttributedStringBuilder().style(AttributedStyle.DEFAULT.background(AttributedStyle.GREEN)).append("Success").style(AttributedStyle.DEFAULT).append(" ").style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append(String.format(msg, args)).toAnsi(terminal));
		terminal.flush();
	}

	public String coloredString(Terminal terminal, int color, String s, Object... args) {
		return new AttributedStringBuilder().style(AttributedStyle.DEFAULT.foreground(color)).append(String.format(s, args)).style(AttributedStyle.DEFAULT).toAnsi(terminal);
	}

	public List<String> askForMultiSelection(Terminal terminal, List<ImmutablePair<String, Boolean>> options, int color) {
		enum OPERATION {
			UP, DOWN, TICK, ACCEPT
		}
		terminal.enterRawMode();
		terminal.puts(InfoCmp.Capability.keypad_xmit);
		val keys = new KeyMap<OPERATION>();
		val reader = new BindingReader(terminal.reader());
		val selectedOffsets = new ArrayList<Integer>();
		for (int i = 0; i < options.size(); i++) {
			if (i == options.size() - 1) {
				if (options.get(i).getValue()) {
					terminal.writer().print(coloredString(terminal, color, "[x] %s", options.get(i).getKey()));
					selectedOffsets.add(options.size() - 1 - i);
				} else {
					terminal.writer().print(String.format("[ ] %s", options.get(i).getKey()));
				}
			} else {
				if (options.get(i).getValue()) {
					terminal.writer().println(coloredString(terminal, color, "[x] %s", options.get(i).getKey()));
					selectedOffsets.add(options.size() - 1 - i);
				} else {
					terminal.writer().println(String.format("[ ] %s", options.get(i).getKey()));
				}
			}
		}
		terminal.puts(InfoCmp.Capability.carriage_return);
		terminal.flush();
		var offset = 0;
		keys.bind(OPERATION.UP, KeyMap.key(terminal, InfoCmp.Capability.key_up));
		keys.bind(OPERATION.DOWN, KeyMap.key(terminal, InfoCmp.Capability.key_down));
		keys.bind(OPERATION.TICK, List.of(" "));
		keys.bind(OPERATION.ACCEPT, List.of("\r"));
		while (true) {
			val op = reader.readBinding(keys);
			switch (op) {
				case UP -> {
					if (offset < options.size() - 1) {
						offset++;
						terminal.puts(InfoCmp.Capability.cursor_up);
						terminal.flush();
					}
				}
				case DOWN -> {
					if (offset > 0) {
						offset--;
						terminal.puts(InfoCmp.Capability.cursor_down);
						terminal.flush();
					}
				}
				case TICK -> {
					if (selectedOffsets.contains(offset)) {
						selectedOffsets.remove(Integer.valueOf(offset));
						terminal.writer().print(String.format("[ ] %s", options.get(options.size() - 1 - offset).getKey()));
					} else {
						selectedOffsets.add(offset);
						terminal.writer().print(coloredString(terminal, color, "[x] %s", options.get(options.size() - 1 - offset).getKey()));
					}
					terminal.puts(InfoCmp.Capability.carriage_return);
					terminal.flush();
				}
				case ACCEPT -> {
					for (int i = 0; i < offset + 1; i++) {
						terminal.puts(InfoCmp.Capability.cursor_down);
						terminal.flush();
					}
					return selectedOffsets.stream().sorted().map((i) -> options.get(options.size() - 1 - i)).map(ImmutablePair::getKey).toList();
				}
			}
		}
	}
}
