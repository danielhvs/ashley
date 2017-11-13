
package danielhabib.sandbox.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import danielhabib.factory.TextFactory;
import danielhabib.sandbox.ScreenEnum;
import danielhabib.sandbox.ScreenManager;
import danielhabib.sandbox.components.BoundsComponent;
import danielhabib.sandbox.components.CountComponent;
import danielhabib.sandbox.components.GeneralCallback;
import danielhabib.sandbox.components.MovementComponent;
import danielhabib.sandbox.components.PlatformComponent;
import danielhabib.sandbox.components.RotationComponent;
import danielhabib.sandbox.components.SnakeComponent;
import danielhabib.sandbox.components.StateComponent;
import danielhabib.sandbox.components.TimeoutComponent;
import danielhabib.sandbox.components.TransformComponent;
import danielhabib.sandbox.types.PlatformType;

public class CollisionSystem extends EntitySystem {
	private ComponentMapper<BoundsComponent> bounds;

	private Engine engine;
	private ImmutableArray<Entity> snakes;
	private ImmutableArray<Entity> platformComponents;
	private ComponentMapper<CountComponent> counts;

	public CollisionSystem() {

		bounds = ComponentMapper.getFor(BoundsComponent.class);
		counts = ComponentMapper.getFor(CountComponent.class);
	}

	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		snakes = engine.getEntitiesFor(
				Family.all(SnakeComponent.class, StateComponent.class, BoundsComponent.class, TransformComponent.class)
						.get());
		platformComponents = engine.getEntitiesFor(
				Family.all(PlatformComponent.class, BoundsComponent.class, TransformComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		SnakeSystem snakeSystem = engine.getSystem(SnakeSystem.class);
		for (Entity snake : snakes) {
			checkSnakeCollision(snakeSystem, snake);
		}
	}

	private void checkSnakeCollision(SnakeSystem snakeSystem, Entity snake) {
		for (Entity platform : platformComponents) {
			BoundsComponent snakeBound = bounds.get(snake);
			BoundsComponent platformBound = bounds.get(platform);
			if (snakeBound.bounds.overlaps(platformBound.bounds)) {
				PlatformComponent platformComponent = platform
						.getComponent(PlatformComponent.class);
				PlatformType type = platformComponent.type;
				platformComponent.hit();
				if (type == PlatformType.FRUIT) {
					engine.removeEntity(platform);
					snakeSystem.grow(snake);
					CountComponent countComponent = counts.get(snake);
					TextFactory.addCountingAnimation(countComponent.fruitsLabel,
							String.valueOf(++countComponent.fruits), Color.WHITE, 5, 15);
				} else if (type == PlatformType.POISON) {
					engine.removeEntity(platform);
					snakeSystem.removeTail(snake);
				} else if (type == PlatformType.SPEED) {
					snakeSystem.increaseSpeed(snake);
					getEngine().removeEntity(platform);
				} else if (type == PlatformType.WALL) {
					ImmutableArray<Entity> entities = getEngine().getEntities();
					for (Entity entity : entities) {
						float random = MathUtils.random();
						entity.add(
								new RotationComponent(random < 0.5f ? random : -random));
						entity.remove(SnakeComponent.class);
						entity.remove(MovementComponent.class
);
					}
					snake.add(new TimeoutComponent(2f,
							new GeneralCallback() {
						@Override
						public void execute() {
							getEngine().removeAllEntities();
							ScreenManager.getInstance().showScreen(ScreenEnum.MAIN_MENU);
						}
					}));
				}
			}
		}
	}
}
