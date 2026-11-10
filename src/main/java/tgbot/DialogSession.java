package tgbot;

import java.util.List;

/** Состояние диалога одного пользователя. */
public class DialogSession {
    /** Текущий шаг диалога. */
    private DialogStep step = DialogStep.IDLE;
    /** Название рецепта, который добавляют или редактируют. */
    private String draftName;
    /** Ингредиенты рецепта, который добавляют или редактируют. */
    private List<String> draftIngredients;
    /** ID редактируемого рецепта. */
    private int editedId;

    /** Начинает добавление рецепта с указанным названием. */
    public void startAdd(String name) {
        draftName = name;
        step = DialogStep.ADD_INGREDIENTS;
    }

    /** Запоминает ингредиенты нового рецепта и переходит к вводу инструкции. */
    public void setIngredients(List<String> ingredients) {
        draftIngredients = ingredients;
        step = DialogStep.ADD_INSTRUCTION;
    }

    /** Начинает редактирование рецепта с указанным ID. */
    public void startEdit(int id) {
        editedId = id;
        step = DialogStep.EDIT_NAME;
    }

    /** Запоминает новое название и переходит к вводу ингредиентов. */
    public void setEditedName(String name) {
        draftName = name;
        step = DialogStep.EDIT_INGREDIENTS;
    }

    /** Запоминает новые ингредиенты и переходит к вводу инструкции. */
    public void setEditedIngredients(List<String> ingredients) {
        draftIngredients = ingredients;
        step = DialogStep.EDIT_INSTRUCTION;
    }

    /** Сбрасывает диалог в начальное состояние. */
    public void reset() {
        step = DialogStep.IDLE;
        draftName = null;
        draftIngredients = null;
        editedId = 0;
    }

    /** @return текущий шаг диалога */
    public DialogStep getStep() {
        return step;
    }

    /** @return название рецепта в работе */
    public String getDraftName() {
        return draftName;
    }

    /** @return ингредиенты рецепта в работе */
    public List<String> getDraftIngredients() {
        return draftIngredients;
    }

    /** @return ID редактируемого рецепта */
    public int getEditedId() {
        return editedId;
    }
}