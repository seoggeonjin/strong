package net.ua;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.ua.commands.Commands;
import net.ua.commands.cdc2;
import net.ua.commands.t1;
import net.ua.commands.tm1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.text.ClickEvent.Action.COPY_TO_CLIPBOARD;
import static net.ua.custom.custom.register;
import static net.ua.custom.items.groups.registerGroup;

public class Strong implements ModInitializer {
	private static final AtomicReference<Float> customTickrate = new AtomicReference<>(20.0F);
	public static final Map<Integer, String> keys = new HashMap<>();
	public static final Logger LOGGER = LoggerFactory.getLogger("strong");
	public static final String MOD_ID = "strong";
	// public static final EntityType<BreezeEntity> BREEZE = register2("breeze2", EntityType.Builder.create(BreezeEntity::new, SpawnGroup.MONSTER).makeFireImmune().setDimensions(0.6f, 1.8f).maxTrackingRange(8));

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, server) -> {
			cdc2.register(dispatcher);
			tm1.register(dispatcher);
			t1.register(dispatcher);
			t1.register2(dispatcher);
		});
		CommandRegistrationCallback.EVENT.register(Commands::register);
		register();
		registerGroup();
		LOGGER.info("Hello Fabric world!");
	}
	public static void log(String s) {
		LOGGER.info(s);
	}
	public static void log(Object o) {
		LOGGER.info(String.valueOf(o));
	}
	private static float oldTPSMaster = 20.0F;
	public static void updateTPSMaster() {
		if (customTickrate.get() != oldTPSMaster) {
			TPSMaster.change(customTickrate.get());
			oldTPSMaster = customTickrate.get();
		}
	}
	static {
		keys.put(0, "ktvavlchddfxusjkqpgwjtgbbykukjhsishjdtpwrahlnyovjr");
		keys.put(1, "itnrdpghxxkdfwepdamvmgayzrdpisegdrrvniegafwkgplwdb");
		keys.put(2, "ozacekwjtlrayhozjnglyjruxgtvdbxpdoulyngcdwiplukazp");
		keys.put(3, "xdthplxcobtgqdrvrdemlwcbhikzbvylreolaxujwpftytawdt");
		keys.put(4, "efzcehfhasvmqvwiscpncrpsrpdlojlgfmoqmxmetrbigmolvt");
		keys.put(5, "qnfduosnnzbwvwnnokwhqkfmuzhisidmfqqraprrwjowmfrchz");
		keys.put(6, "chnnrqolglipgqbshpxkgbeaitkogtckqyfwxyqqvmvfobsegf");
		keys.put(7, "selzrxmkgeldgrciigltvhujocvhfqbrrtgtcpvdjwhkizphol");
		keys.put(8, "xsmytqeyguixjpsjevqjjfeovazswpmjwtqfgqptpebtmqdjbz");
		keys.put(9, "ovjgadeepyiaiolhlpcecksxqducaebazpydvryixwfmifvfzp");
	}
}