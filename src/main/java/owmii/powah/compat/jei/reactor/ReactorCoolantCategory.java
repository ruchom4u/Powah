package owmii.powah.compat.jei.reactor;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import owmii.powah.Powah;
import owmii.powah.api.PowahAPI;
import owmii.powah.block.generator.reactor.Reactors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReactorCoolantCategory implements IRecipeCategory<ReactorCoolantCategory.Recipe> {
    public static final ResourceLocation GUI_BACK = new ResourceLocation(Powah.MOD_ID, "textures/gui/jei/misc.png");
    public static final ResourceLocation ID = new ResourceLocation(Powah.MOD_ID, ".reactor.coolant");
    private final IDrawable background;
    private final IDrawable icon;
    private final String localizedName;

    public ReactorCoolantCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(GUI_BACK, 0, 0, 160, 24).addPadding(1, 0, 0, 0).build();
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(Reactors.BASIC.get()));
        this.localizedName = I18n.format("gui.powah.jei.category.coolant");
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public Class<? extends Recipe> getRecipeClass() {
        return Recipe.class;
    }

    @Override
    public String getTitle() {
        return this.localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setIngredients(Recipe recipe, IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, new FluidStack(recipe.getFluid(), 1000));
        if (!Items.BUCKET.equals(recipe.bucket)) {
            ingredients.setInput(VanillaTypes.ITEM, new ItemStack(recipe.bucket));
        }
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, Recipe recipe, IIngredients ingredients) {
        IGuiFluidStackGroup fluidStack = iRecipeLayout.getFluidStacks();
        fluidStack.init(0, true, 4, 5);
        fluidStack.set(ingredients);
    }

    @Override
    public void draw(Recipe recipe, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.fontRenderer.drawString(I18n.format("info.powah.coldness", "" + TextFormatting.BLUE + recipe.coldness), 30.0F, 9.0F, 0x444444);
    }

    public static class Maker {
        public static List<Recipe> getBucketRecipes(IIngredientManager ingredientManager) {
            Collection<ItemStack> allItemStacks = ingredientManager.getAllIngredients(VanillaTypes.ITEM);
            List<Recipe> recipes = new ArrayList<>();

            allItemStacks.forEach(stack -> {
                if (stack.getItem() instanceof BucketItem && !(stack.getItem() instanceof FishBucketItem)) {
                    BucketItem bucket = (BucketItem) stack.getItem();
                    Fluid fluid = bucket.getFluid();
                    if (PowahAPI.REACTOR_COOLANTS.containsKey(fluid)) {
                        recipes.add(new Recipe(bucket, PowahAPI.getReactorCoolant(fluid)));
                    }
                }
            });

            List<Fluid> fluids = new ArrayList<>(PowahAPI.REACTOR_COOLANTS.keySet());
            recipes.forEach(recipe -> {
                fluids.remove(recipe.fluid);
            });

            return recipes;
        }
    }

    public static class Recipe {
        private final Fluid fluid;
        private final BucketItem bucket;
        private final int coldness;

        public Recipe(BucketItem bucket, int coldness) {
            this.bucket = bucket;
            this.fluid = bucket.getFluid();
            this.coldness = coldness;
        }

        public Recipe(Fluid fluid, int coldness) {
            this.bucket = (BucketItem) Items.BUCKET;
            this.fluid = fluid;
            this.coldness = coldness;
        }

        public BucketItem getBucket() {
            return this.bucket;
        }

        public Fluid getFluid() {
            return this.fluid;
        }

        public int getColdness() {
            return this.coldness;
        }
    }
}
