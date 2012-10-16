package eu.lestard.snakefx.core;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import eu.lestard.snakefx.util.Callback;

public class SnakeTest {
	private Snake snake;

	private Grid gridMock;

	private static final int X = 4;
	private static final int Y = 2;

	private Callback pointsEventCallback;

	@Before
	public void setUp() {
		gridMock = mock(Grid.class);

		pointsEventCallback = mock(Callback.class);

		snake = new Snake(gridMock, X, Y);

		snake.addPointsEventListener(pointsEventCallback);
	}

	@Test
	public void testInitialization() {
		Field field = mock(Field.class);

		when(gridMock.getXY(X, Y)).thenReturn(field);

		snake.init();

		verify(gridMock, times(1)).getXY(X, Y);
		verify(field, times(1)).changeState(State.HEAD);

		assertThat(snake.getHead()).isEqualTo(field);

		// The direction of the snake is UP on start.
		Direction direction = currentDirectionFromSnake();
		assertThat(direction).isEqualTo(Direction.UP);
	}

	@Test
	public void testChangeDirection() {
		snake.changeDirection(Direction.LEFT);
		Direction direction = nextDirectionFromSnake();

		assertThat(direction).isEqualTo(Direction.LEFT);
	}

	/**
	* When the new direction has the same orientation as the old one ( both are
	* horizontal or both are vertical) no change of the direction should be
	* made.
	*
	* Otherwise the head of the snake would move directly into the tail.
	*
	*
	* But if the player pressed LEFT and then DOWN faster then the gap between
	* two frames, then the Snake would make a 180 degree turnaround. The LEFT
	* keypress wouldn't be filtered out because LEFT has another orientation
	* then UP and the DOWN keypress wouldn't be filtered out because LEFT
	* (which is the "next direction" now) has another orientation then DOWN.
	*
	* To prevend this we have two variables for the direction: "nextDirection"
	* and "currentDirection". When the player likes to change the direction,
	* only nextDirection is changed but he test whether the orientation is the
	* same is done with the "currentDirection". When the snake moves, the
	* "currentDirection" variable gets the value from "nextDirection".
	*
	*/
	@Test
	public void testChangeDirectionNewHasSameOrientationAsOld() {
		Field head = mock(Field.class);
		when(gridMock.getXY(X, Y)).thenReturn(head);

		Field newHead = mock(Field.class);
		when(newHead.getState()).thenReturn(State.EMPTY);
		when(gridMock.getFromDirection(head, Direction.LEFT)).thenReturn(
		newHead);

		// Snake is initialized with currentDirection=UP and nextDirection=UP
		snake.init();


		snake.changeDirection(Direction.DOWN);

		// currentDirection and nextDirection is still UP because the
		// orientation is the same
		assertThat(nextDirectionFromSnake()).isEqualTo(Direction.UP);
		assertThat(currentDirectionFromSnake()).isEqualTo(Direction.UP);


		snake.changeDirection(Direction.LEFT);
		// the nextDirection is now changed...
		assertThat(nextDirectionFromSnake()).isEqualsToByComparingFields(
		Direction.LEFT);
		// ... the currentDirection is still the old one. It is only changed
		// when the
		// snake moves.
		assertThat(currentDirectionFromSnake()).isEqualTo(Direction.UP);


		snake.changeDirection(Direction.DOWN);
		// nextDirection is not changed as the currentDirection is still UP and
		// has the same orientation as DOWN
		assertThat(nextDirectionFromSnake()).isEqualTo(Direction.LEFT);
		assertThat(currentDirectionFromSnake()).isEqualTo(Direction.UP);


		snake.move();

		assertThat(nextDirectionFromSnake()).isEqualTo(Direction.LEFT);
		// now the currentDirection has changed.
		assertThat(currentDirectionFromSnake()).isEqualTo(Direction.LEFT);
	}

	@Test
	public void testMove() {
		Field oldHead = mock(Field.class);
		when(oldHead.getState()).thenReturn(State.EMPTY);
		when(gridMock.getXY(X, Y)).thenReturn(oldHead);

		snake.init();

		Field newHead = mock(Field.class);
		when(newHead.getState()).thenReturn(State.EMPTY);
		when(gridMock.getFromDirection(oldHead, Direction.UP)).thenReturn(
				newHead);

		snake.move();

		assertThat(snake.getHead()).isEqualTo(newHead);

		verify(oldHead).changeState(State.EMPTY);
	}

	/**
	* When the snake moves to a field that has the state "FOOD" the snake
	* should grow by 1 field.
	*/
	@Test
	public void testGrow() {
		Field field1 = new Field(0, 3, 10);
		// at the start field1 is the head
		when(gridMock.getXY(X, Y)).thenReturn(field1);

		// field2 is above field1
		Field field2 = new Field(0, 2, 10);
		field2.changeState(State.FOOD);
		when(gridMock.getFromDirection(field1, Direction.UP))
				.thenReturn(field2);

		// field3 is above field2
		Field field3 = new Field(0, 1, 10);
		when(gridMock.getFromDirection(field2, Direction.UP))
				.thenReturn(field3);

		snake.init();

		snake.move();

		// the head of the snake is now on field2
		assertThat(snake.getHead()).isEqualTo(field2);

		// field1 is now a part of the tail
		assertThat(field1.getState()).isEqualTo(State.TAIL);

		// The pointsEvent has to be fired
		verify(pointsEventCallback, times(1)).call();

		// Now the snake is moving another field forward. This time the new
		// field (field3)
		// is empty.

		snake.move();

		// field3 becomes the new head
		assertThat(snake.getHead()).isEqualTo(field3);

		// field2 becomes the tail
		assertThat(field2.getState()).isEqualTo(State.TAIL);

		// field1 is now empty
		assertThat(field1.getState()).isEqualTo(State.EMPTY);

	}

	private Direction nextDirectionFromSnake() {
		return (Direction) Whitebox.getInternalState(snake, "nextDirection");
	}

	private Direction currentDirectionFromSnake() {
		return (Direction) Whitebox.getInternalState(snake, "currentDirection");
	}
}