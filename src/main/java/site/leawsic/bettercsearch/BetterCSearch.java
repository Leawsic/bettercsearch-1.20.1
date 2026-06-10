package site.leawsic.bettercsearch;

import net.fabricmc.api.ModInitializer;
import site.leawsic.bettercsearch.command.ModCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterCSearch implements ModInitializer {
	public static final String MOD_ID = "bettercsearch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModCommands.register();
		LOGGER.info("BetterCSearch initialized");
	}
}