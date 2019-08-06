package reborncore.common.crafting.ingredient;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.stream.Collectors;

public class TagIngredient extends RebornIngredient {

	private final Identifier tagIdentifier;
	private final Tag<Item> tag;

	public TagIngredient(Identifier tagIdentifier, Tag<Item> tag) {
		super(IngredientManager.TAG_RECIPE_TYPE);
		this.tagIdentifier = tagIdentifier;
		this.tag = tag;
	}

	@Override
	public boolean test(ItemStack itemStack) {
		return itemStack.getItem().isIn(tag);
	}

	@Override
	public Ingredient getPreview() {
		return Ingredient.fromTag(tag);
	}

	@Override
	public List<ItemStack> getPreviewStacks() {
		return tag.values().stream().map(ItemStack::new).collect(Collectors.toList());
	}

	public static RebornIngredient deserialize(JsonObject json) {
		if(json.has("server_sync")){
			Identifier tagIdent = new Identifier(JsonHelper.getString(json, "tag_identifier"));
			Tag.Builder<Item> tagBuilder = Tag.Builder.create();
			for (int i = 0; i < JsonHelper.getInt(json, "items"); i++) {
				Identifier identifier = new Identifier(JsonHelper.getString(json, "item_" + i));
				Item item = Registry.ITEM.get(identifier);
				Validate.isTrue(item != Items.AIR, "item cannot be air");
				tagBuilder.add(item);
			}
			return new TagIngredient(tagIdent, tagBuilder.build(tagIdent));
		}

		Identifier identifier = new Identifier(JsonHelper.getString(json, "tag"));
		Tag<Item> tag = ItemTags.getContainer().get(identifier);
		if (tag == null) {
			throw new JsonSyntaxException("Unknown item tag '" + identifier + "'");
		}
		return new TagIngredient(identifier, tag);
	}

	@Override
	public JsonObject toJson() {
		//Tags are not synced across the server so we sync all the items
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("server_sync", true);

		Item[] items = tag.values().toArray(new Item[0]);
		jsonObject.addProperty("items", items.length);
		for (int i = 0; i < items.length; i++) {
			jsonObject.addProperty("item_" + i, Registry.ITEM.getId(items[i]).toString());
		}

		jsonObject.addProperty("tag_identifier", tagIdentifier.toString());
		return jsonObject;
	}
}