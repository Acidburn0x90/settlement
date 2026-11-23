package com.settlement.mc.persistence;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSoilAttachmentTest {

	@Test
	@DisplayName("default attachment starts with full 1.0 fertility and 0 experience")
	void defaultStateStartsFertileWithZeroExperience() {
		ChunkSoilAttachment soil = new ChunkSoilAttachment();

		assertEquals(1.0, soil.getFertility(), 1e-9);
		assertEquals(0, soil.getCultivationExperience());
	}

	@Test
	@DisplayName("MAP_CODEC round-trips fertility and cultivationExperience through NbtOps")
	void mapCodecRoundTripPreservesState() {
		ChunkSoilAttachment original = new ChunkSoilAttachment(0.65, 14);

		Tag nbt = ChunkSoilAttachment.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		ChunkSoilAttachment restored = ChunkSoilAttachment.MAP_CODEC.codec().parse(NbtOps.INSTANCE, nbt).getOrThrow();

		assertEquals(0.65, restored.getFertility(), 1e-9);
		assertEquals(14, restored.getCultivationExperience());
	}

	@Test
	@DisplayName("empty or out-of-range NBT handles defaults and validation errors safely")
	void codecValidatesBoundsAndDefaults() {
		ChunkSoilAttachment defaulted = ChunkSoilAttachment.MAP_CODEC.codec()
				.parse(NbtOps.INSTANCE, new CompoundTag())
				.getOrThrow();
		assertEquals(1.0, defaulted.getFertility(), 1e-9);
		assertEquals(0, defaulted.getCultivationExperience());

		CompoundTag invalidFertility = new CompoundTag();
		invalidFertility.putDouble("fertility", 1.5);
		DataResult<ChunkSoilAttachment> badFertility = ChunkSoilAttachment.MAP_CODEC.codec()
				.parse(NbtOps.INSTANCE, invalidFertility);
		assertTrue(badFertility.isError());

		CompoundTag negativeExp = new CompoundTag();
		negativeExp.putInt("cultivation_experience", - 1);
		DataResult<ChunkSoilAttachment> badExp = ChunkSoilAttachment.MAP_CODEC.codec()
				.parse(NbtOps.INSTANCE, negativeExp);
		assertTrue(badExp.isError());
	}

	@Test
	@DisplayName("applyHarvestExhaustion degrades fertility clamped to 0.0 and increments experience")
	void harvestExhaustionClampsAndIncrementsExperience() {
		ChunkSoilAttachment soil = new ChunkSoilAttachment(0.25, 3);

		soil.applyHarvestExhaustion(0.10);
		assertEquals(0.15, soil.getFertility(), 1e-9);
		assertEquals(4, soil.getCultivationExperience());

		soil.applyHarvestExhaustion(0.50);
		assertEquals(0.0, soil.getFertility(), 1e-9);
		assertEquals(5, soil.getCultivationExperience());
	}

	@Test
	@DisplayName("applyFallowRecovery restores fertility clamped to 1.0 without altering experience")
	void fallowRecoveryClampsToOne() {
		ChunkSoilAttachment soil = new ChunkSoilAttachment(0.80, 7);

		soil.applyFallowRecovery(0.15);
		assertEquals(0.95, soil.getFertility(), 1e-9);
		assertEquals(7, soil.getCultivationExperience());

		soil.applyFallowRecovery(0.50);
		assertEquals(1.0, soil.getFertility(), 1e-9);
		assertEquals(7, soil.getCultivationExperience());
	}

	@Test
	@DisplayName("constructor, mutators, and mutate() reject invalid, NaN, infinite, and null inputs")
	void invalidInputsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new ChunkSoilAttachment(- 0.01, 0));
		assertThrows(IllegalArgumentException.class, () -> new ChunkSoilAttachment(1.01, 0));
		assertThrows(IllegalArgumentException.class, () -> new ChunkSoilAttachment(Double.NaN, 0));
		assertThrows(IllegalArgumentException.class, () -> new ChunkSoilAttachment(Double.POSITIVE_INFINITY, 0));
		assertThrows(IllegalArgumentException.class, () -> new ChunkSoilAttachment(0.5, - 1));

		ChunkSoilAttachment soil = new ChunkSoilAttachment();
		assertThrows(IllegalArgumentException.class, () -> soil.applyHarvestExhaustion(- 0.1));
		assertThrows(IllegalArgumentException.class, () -> soil.applyHarvestExhaustion(Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> soil.applyHarvestExhaustion(Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class, () -> soil.applyFallowRecovery(- 0.1));
		assertThrows(IllegalArgumentException.class, () -> soil.applyFallowRecovery(Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> soil.applyFallowRecovery(Double.NEGATIVE_INFINITY));

		assertThrows(IllegalArgumentException.class, () -> ChunkSoilAttachment.mutate(null, s -> {}));

		ChunkSoilAttachment maxExperience = new ChunkSoilAttachment(0.5, Integer.MAX_VALUE);
		assertThrows(ArithmeticException.class, () -> maxExperience.applyHarvestExhaustion(0.1));
	}
}
