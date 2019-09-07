package space.game.asteroidsDemo.asteroid;

import org.jetbrains.annotations.NotNull;
import space.engine.barrier.future.Future;
import space.engine.buffer.Allocator;
import space.engine.buffer.AllocatorStack.AllocatorFrame;
import space.engine.buffer.array.ArrayBufferFloat;
import space.engine.freeableStorage.Freeable;
import space.engine.freeableStorage.Freeable.FreeableWrapper;
import space.engine.indexmap.IndexMap;
import space.engine.indexmap.IndexMap.Entry;
import space.engine.indexmap.IndexMapArray;
import space.engine.vector.Translation;
import space.engine.vector.Vector3;
import space.engine.vulkan.VkBuffer;
import space.engine.vulkan.VkCommandBuffer;
import space.engine.vulkan.managed.descriptorSet.ManagedDescriptorSetPool;
import space.engine.vulkan.managed.renderPass.ManagedFrameBuffer;
import space.engine.vulkan.managed.renderPass.ManagedRenderPass.Callback;
import space.engine.vulkan.vma.VmaMappedBuffer;
import space.game.asteroidsDemo.renderPass.AsteroidDemoInfos;
import space.game.asteroidsDemo.renderPass.AsteroidDemoRenderPass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
import static org.lwjgl.vulkan.VK10.*;
import static space.engine.Empties.EMPTY_OBJECT_ARRAY;
import static space.engine.barrier.Barrier.*;
import static space.engine.buffer.Allocator.heap;
import static space.engine.freeableStorage.Freeable.addIfNotContained;
import static space.engine.primitive.Primitives.FP32;

public class AsteroidRenderer implements FreeableWrapper, Callback<AsteroidDemoInfos> {
	
	private final AsteroidDemoRenderPass renderPass;
	private final AsteroidPipeline asteroidPipeline;
	private final AsteroidModel[] asteroidModels;
	private final IndexMap<Collection<Asteroid>> asteroids = new IndexMapArray<>();
	
	private final ManagedDescriptorSetPool descriptorSetPool;
	
	public AsteroidRenderer(AsteroidDemoRenderPass renderPass, AsteroidPipeline asteroidPipeline, AsteroidModel[] asteroidModels, Object[] parents) {
		this.renderPass = renderPass;
		this.asteroidPipeline = asteroidPipeline;
		this.asteroidModels = asteroidModels;
		
		this.storage = Freeable.createDummy(this, addIfNotContained(parents, renderPass, asteroidPipeline));
		
		this.descriptorSetPool = new ManagedDescriptorSetPool(renderPass.device(), asteroidPipeline.descriptorSetLayout(), asteroidModels.length, new Object[] {this});
	}
	
	public void addAsteroid(Asteroid asteroid) {
		asteroids.computeIfAbsent(asteroid.modelId, ArrayList::new).add(asteroid);
	}
	
	public int variations() {
		return asteroidModels.length;
	}
	
	@Override
	public @NotNull List<Future<VkCommandBuffer[]>> getCmdBuffers(@NotNull ManagedFrameBuffer<AsteroidDemoInfos> render, AsteroidDemoInfos infos) {
		List<? extends Future<ArrayList<VkCommandBuffer>>> futures = asteroids.entrySet().stream().filter(entry -> entry.getValue() != null).map(entry -> nowFuture(() -> {
			int index = entry.getIndex();
			AsteroidModel model = asteroidModels[index];
			
			IndexMap<Collection<Translation>> sorted = new IndexMapArray<>();
			for (Asteroid asteroid : entry.getValue()) {
				Translation translation = asteroid.toTranslation(infos.frameTimeSeconds).build();
				
				float distanceToCamera = Vector3.distance(translation.offset, infos.camera.position);
				int i = 0;
				for (; i < model.minDistance.length; i++)
					if (distanceToCamera < model.minDistance[i])
						break;
				//too far away for any render
				if (i == model.minDistance.length)
					continue;
				sorted.computeIfAbsent(i, ArrayList::new).add(translation);
			}
			
			ArrayList<VkCommandBuffer> cmdBuffers = new ArrayList<>();
			for (Entry<Collection<Translation>> entry2 : sorted.entrySet()) {
				Collection<Translation> translations = entry2.getValue();
				if (translations == null)
					continue;
				
				VkCommandBuffer cmd = render.queue().commandPool().allocCommandBuffer(VK_COMMAND_BUFFER_LEVEL_SECONDARY, EMPTY_OBJECT_ARRAY);
				cmd.record(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT | VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT, render.inheritanceInfo(infos, renderPass.subpassRender), () -> {
							   float[] instanceData = new float[translations.size() * 28];
							   Iterator<Translation> iter = translations.iterator();
							   for (int i = 0; iter.hasNext(); i++) {
								   Translation translation = iter.next();
								   translation.matrix.write4Aligned(instanceData, i * 28);
								   translation.matrix.inverse().write4Aligned(instanceData, i * 28 + 12);
								   translation.offset.write4Aligned(instanceData, i * 28 + 24);
							   }
					
							   VmaMappedBuffer instanceBuffer = VmaMappedBuffer.alloc(0, instanceData.length * FP32.bytes, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 0, VMA_MEMORY_USAGE_CPU_TO_GPU, renderPass.device(), new Object[] {infos});
							   try (AllocatorFrame frame = Allocator.frame()) {
								   instanceBuffer.uploadData(ArrayBufferFloat.alloc(heap(), instanceData, new Object[] {frame}));
							   }
					
							   asteroidPipeline.bindPipeline(cmd, descriptorSetPool.sets()[index], infos);
							   VkBuffer vertexBuffer = model.models[entry2.getIndex()];
							   vkCmdBindVertexBuffers(cmd, 0, new long[] {
									   vertexBuffer.address(),
									   instanceBuffer.address()
							   }, new long[] {
									   0,
									   0
							   });
					vkCmdDraw(cmd, (int) (vertexBuffer.sizeOf() / (FP32.bytes * 6)), translations.size(), 0, 0);
					
							   return instanceBuffer;
						   }
				);
				infos.frameDone.addHook(cmd::free);
				cmdBuffers.add(cmd);
			}
			return cmdBuffers;
		})).collect(Collectors.toUnmodifiableList());
		
		Future<VkCommandBuffer[]> renderFuture = when(futures).thenFuture(() -> futures
				.stream()
				.map(Future::assertGet)
				.flatMap(ArrayList::stream)
				.toArray(VkCommandBuffer[]::new)
		);
		
		return List.of(
				renderFuture
		);
	}
	
	//storage
	private final Freeable storage;
	
	@Override
	public @NotNull Freeable getStorage() {
		return storage;
	}
	
	public static class AsteroidModel {
		
		private final VkBuffer[] models;
		private final float[] minDistance;
		
		public AsteroidModel(VkBuffer[] models, float[] minDistance) {
			this.models = models;
			this.minDistance = minDistance;
		}
	}
}
