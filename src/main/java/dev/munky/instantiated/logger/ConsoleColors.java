package dev.munky.instantiated.logger;
/*
https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
 */
public enum ConsoleColors {
		RESET("\u001B[0m"),
		FG_BLACK("\u001B[30m"),
		BOLD("\u001B[1m"),
		RAPID_BLINK("\u001B[6m"),
		FG_RED("\u001B[38;5;9m"),
		FG_GREEN("\u001B[32m"),
		FG_YELLOW("\u001B[33m"),
		FG_BLUE("\u001B[38;5;12m"),
		FG_PURPLE("\u001B[38;5;207m"),
		FG_CYAN("\u001B[36m"),
		FG_WHITE("\u001B[37m"),
		BG_RED("\u001B[41m"),
		FG_DARK_RED("\u001B[38;5;88m");
		public final String code;
		ConsoleColors(String code) {
                this.code = code;
		}
        public String toString(){
                return this.code;
        }
}