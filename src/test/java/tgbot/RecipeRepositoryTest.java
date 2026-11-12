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

    /** У каждого пользователя нумерация начинается с 1. */
    @Test
    void shouldNumberFromOneForEachUser() {
        RecipeRepository repository = new RecipeRepository();
        Recipe first = repository.add(1, "Паста", List.of("сыр"), "Сварить");
        Recipe second = repository.add(1, "Омлет", List.of("яйца"), "Пожарить");
        Recipe other = repository.add(2, "Борщ", List.of("свёкла"), "Варить");

        Assertions.assertEquals(1, first.getId());
        Assertions.assertEquals(2, second.getId());
        Assertions.assertEquals(1, other.getId());
    }

    /** После удаления рецепта следующие сдвигаются, нумерация идёт без пропусков. */
    @Test
    void shouldRenumberAfterRemove() {
        RecipeRepository repository = new RecipeRepository();
        repository.add(1, "Первый", List.of("а"), "1");
        repository.add(1, "Второй", List.of("б"), "2");
        repository.add(1, "Третий", List.of("в"), "3");
        repository.add(1, "Четвёртый", List.of("г"), "4");

        repository.remove(1, 2);

        List<Recipe> all = repository.findAll(1);
        Assertions.assertEquals(3, all.size());
        Assertions.assertEquals("Первый", repository.find(1, 1).orElseThrow().getName());
        Assertions.assertEquals("Третий", repository.find(1, 2).orElseThrow().getName());
        Assertions.assertEquals("Четвёртый", repository.find(1, 3).orElseThrow().getName());
        Assertions.assertEquals(2, all.get(1).getId());
        Assertions.assertEquals(3, all.get(2).getId());
        Assertions.assertTrue(repository.find(1, 4).isEmpty());
    }

    /** После удаления единственного рецепта следующий снова получает номер 1. */
    @Test
    void shouldReuseNumberOneAfterRemovingOnlyRecipe() {
        RecipeRepository repository = new RecipeRepository();
        Recipe first = repository.add(1, "Паста", List.of("сыр"), "Сварить");
        repository.remove(1, first.getId());

        Recipe second = repository.add(1, "Омлет", List.of("яйца"), "Пожарить");

        Assertions.assertTrue(repository.find(1, 2).isEmpty());
        Assertions.assertEquals(1, second.getId());
    }

    /** Удаление у одного пользователя не затрагивает рецепты другого. */
    @Test
    void shouldNotAffectOtherUserOnRemove() {
        RecipeRepository repository = new RecipeRepository();
        repository.add(1, "Паста", List.of("сыр"), "Сварить");
        repository.add(2, "Борщ", List.of("свёкла"), "Варить");

        repository.remove(1, 1);

        Assertions.assertTrue(repository.findAll(1).isEmpty());
        Assertions.assertEquals("Борщ", repository.find(2, 1).orElseThrow().getName());
    }

    /** Удаление несуществующего рецепта ничего не ломает. */
    @Test
    void shouldIgnoreRemoveOfMissingRecipe() {
        RecipeRepository repository = new RecipeRepository();
        repository.add(1, "Паста", List.of("сыр"), "Сварить");

        repository.remove(1, 5);
        repository.remove(1, 0);
        repository.remove(3, 1);

        Assertions.assertEquals(1, repository.findAll(1).size());
    }
}