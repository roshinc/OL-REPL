package dev.roshin.openliberty.repl.util;

import com.google.common.base.Preconditions;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;

public class TerminalUtils {

    private static final Logger logger = LoggerFactory.getLogger(TerminalUtils.class);

    private TerminalUtils() {
        // Private constructor
    }

    /**
     * Print error messages to the terminal
     *
     * @param message  The message to print
     * @param terminal The terminal to print to
     */
    public static void printErrorMessages(String message, Terminal terminal) {
        // Print error message, in red
        terminal.writer().println(
                new AttributedStringBuilder()
                        .append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                        .append("\n")
                        .toAnsi()
        );
        // Flush the terminal
        terminal.flush();
    }

    /**
     * Prompt the user for a selection from a list of options
     * <p>
     *     If there is only one option, it will be returned without prompting the user
     *
     * @param terminal             The terminal to use, cannot be null
     * @param options              The list of options to select from, cannot be null or empty
     * @param nameOfThingToSelect  The name of the thing to select, if null, "option" will be used
     * @return                     The selected option
     */
    public static String promptUserForSelection(final Terminal terminal, final List<String> options, final String nameOfThingToSelect) {
        logger.debug("Prompting user for selection from {}", options);

        Preconditions.checkNotNull(terminal, "Terminal cannot be null");
        Preconditions.checkNotNull(options, "Options cannot be null");
        Preconditions.checkArgument(!options.isEmpty(), "Options cannot be empty");

        // If nameOfThingToSelect is null, set it to "option"
        final String nameOfThingToSelectNotNull = nameOfThingToSelect == null ? "option" : nameOfThingToSelect;

        final String prompt = String.format("Select a %s: ", nameOfThingToSelectNotNull);

        logger.debug("Prompting user with: {}", prompt);

        // Create a writer
        final PrintWriter writer = terminal.writer();

        // If there is only one option, return it
        if (options.size() == 1) {
            logger.debug("There is only one {}, using it", nameOfThingToSelectNotNull);
            logger.debug("Returning the only {} found: {}",nameOfThingToSelectNotNull, options.get(0));
            // Inform the user that the only subfolder will be used and which one it is
            writer.println(
                    new AttributedStringBuilder()
                            .append("Using the only ")
                            .append(nameOfThingToSelectNotNull)
                            .append(" found: ")
                            .append(options.get(0), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                            .append("\n")
                            .toAnsi()
            );
            // Flush the terminal
            terminal.flush();
            return options.get(0);
        }

        logger.debug("There are multiple {}, asking the user to select one", nameOfThingToSelectNotNull);

        // Create a LineReader
        final LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Display the list of options with color
        writer.println(
                new AttributedStringBuilder()
                        .append(prompt, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append("\n")
                        .toAnsi()
        );
        for (int i = 0; i < options.size(); i++) {
            terminal.writer().println(
                    new AttributedStringBuilder()
                            .append(String.valueOf(i + 1))
                            .append(". ")
                            .append(options.get(i), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                            .append("\n")
                            .toAnsi()
            );
        }

        // Get the user's selection in a loop
        logger.debug("Waiting for user input");
        String selectedOption;
        while (true) {
            String input = lineReader.readLine("> ");

            try {
                int selection = Integer.parseInt(input);
                if (selection > 0 && selection <= options.size()) {
                    selectedOption = options.get(selection - 1);
                    break;
                } else {
                    writer.println(
                            new AttributedStringBuilder()
                                    .append("Invalid selection, try again.\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                                    .toAnsi()
                    );
                }
            } catch (NumberFormatException e) {
                writer.println(
                        new AttributedStringBuilder()
                                .append("Invalid input, try again.\n", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                                .toAnsi()
                );
            }
        }

        writer.println(
                new AttributedStringBuilder()
                        .append("You selected: ")
                        .append(selectedOption, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                        .append("\n")
                        .toAnsi()
        );
        //Flush the terminal
        terminal.flush();
        logger.debug("User selected: {}", selectedOption);
        return selectedOption;
    }
}
