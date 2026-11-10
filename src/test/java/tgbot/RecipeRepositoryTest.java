package tgbot;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Проверяет хранилище рецептов. */
class RecipeRepositoryTest {
    /** Рецепт одного пользователя недоступен другому. */
    @Test
    void shouldKeepCollectionsSeparate() {
        RecipeRepository repository = new RecipeRepository();
        Recipe recipe = repository.add(1, "Паста", List.of("сыр"), "Сварить");

        Assertions.assertTrue(repository.find(1, recipe.getId()).isPresent());
        Assertions.assertTrue(repository.find(2, recipe.getId()).isEmpty());
        Assertions.assertTrue(repository.findAll(2).isEmpty());
    }

    /** Название ищется без учёта регистра и только у своего пользователя. */
    @Test
    void shouldFindByNameIgnoringCase() {
        RecipeRepository repository = new RecipeRepository();
        repository.add(1, "Паста с сыром", List.of("сыр"), "Сварить");

        Assertions.assertTrue(repository.findByName(1, "паста с сыром").isPresent());
        Assertions.assertTrue(repository.findByName(2, "паста с сыром").isEmpty());
    }

    /** Обновление меняет данные, но сохраняет ID. */
    @Test
    void shouldKeepIdOnUpdate() {
        RecipeRepository repository = new RecipeRepository();
        Recipe recipe = repository.add(1, "Паста", List.of("сыр"), "Сварить");

        repository.update(1, recipe.getId(), "Борщ", List.of("свёкла"), "Варить");

        Recipe updated = repository.find(1, recipe.getId()).orElseThrow();
        Assertions.assertEquals("Борщ", updated.getName());
        Assertions.assertEquals(recipe.getId(), updated.getId());
    }

    /** Удалённый рецепт не находится, его ID не используется повторно. */
    @Test
    void shouldRemoveRecipe() {
        RecipeRepository repository = new RecipeRepository();
        Recipe first = repository.add(1, "Паста", List.of("сыр"), "Сварить");
        repository.remove(1, first.getId());
        Recipe second = repository.add(1, "Омлет", List.of("яйца"), "Пожарить");

        Assertions.assertTrue(repository.find(1, first.getId()).isEmpty());
        Assertions.assertNotEquals(first.getId(), second.getId());
    }
}