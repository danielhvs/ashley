
package danielhabib.sandbox.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;

import danielhabib.sandbox.components.BoundsComponent;
import danielhabib.sandbox.components.PlatformComponent;
import danielhabib.sandbox.components.SnakeBodyComponent;
import danielhabib.sandbox.components.StateComponent;
import danielhabib.sandbox.components.TransformComponent;
import danielhabib.sandbox.types.PlatformType;

public class CollisionSystem extends EntitySystem {
	private ComponentMapper<BoundsComponent> bounds;
	private ComponentMapper<StateComponent> states;

	public static interface CollisionListener {
		public void hit();

		public void ate();

		public void poison();
	}

	private Engine engine;
	private CollisionListener listener;
	private ImmutableArray<Entity> snakes;
	private ImmutableArray<Entity> platformComponents;

	public CollisionSystem(CollisionListener listener) {
		this.listener = listener;

		bounds = ComponentMapper.getFor(BoundsComponent.class);
		states = ComponentMapper.getFor(StateComponent.class);
	}

	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		snakes = engine.getEntitiesFor(Family
				.all(SnakeBodyComponent.class, StateComponent.class, BoundsComponent.class, TransformComponent.class)
				.get());
		platformComponents = engine.getEntitiesFor(
				Family.all(PlatformComponent.class, BoundsComponent.class, TransformComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		SnakeSystem snakeSystem = engine.getSystem(SnakeSystem.class);
		for (Entity snake : snakes) {
			StateComponent state = states.get(snake);
			if (state.get() != SnakeBodyComponent.STATE_REVERTING && state.get() != SnakeBodyComponent.STATE_STOP) {
				checkSnakeCollision(snakeSystem, snake);
			}
		}
	}

	private void checkSnakeCollision(SnakeSystem snakeSystem, Entity snake) {
		for (Entity platform : platformComponents) {
			BoundsComponent snakeBound = bounds.get(snake);
			BoundsComponent platformBound = bounds.get(platform);
			if (snakeBound.bounds.overlaps(platformBound.bounds)) {
				PlatformComponent platformComponent = platform.getComponent(PlatformComponent.class);
				PlatformType type = platformComponent.type;
				if (type == PlatformType.BOING) {
					listener.hit();
					snakeSystem.revert(snake);
				} else if (type == PlatformType.FRUIT) {
					listener.ate();
					engine.removeEntity(platform);
					// snakeSystem.grow(snake);
				} else if (type == PlatformType.POISON) {
					listener.poison();
					engine.removeEntity(platform);
					// snakeSystem.removeTail(snake);
				} else if (type == PlatformType.WALL) {
					// listener.hit();
					// snakeSystem.stop(snake);
				} else if (type == PlatformType.SPEED) {
					listener.hit();
					snakeSystem.increaseSpeed(snake);
					engine.removeEntity(platform);
				} else if (type == PlatformType.HOLE) {
					listener.hit();
					snakeSystem.teleport(snake, platformComponent.other.getComponent(TransformComponent.class).pos);
				}
			}
		}
	}
}
