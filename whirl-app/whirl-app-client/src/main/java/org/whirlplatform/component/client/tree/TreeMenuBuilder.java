package org.whirlplatform.component.client.tree;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sencha.gxt.cell.core.client.SimpleSafeHtmlCell;
import com.sencha.gxt.core.client.Style.SelectionMode;
import com.sencha.gxt.core.client.util.IconHelper;
import com.sencha.gxt.data.client.loader.RpcProxy;
import com.sencha.gxt.data.shared.IconProvider;
import com.sencha.gxt.data.shared.SortDir;
import com.sencha.gxt.data.shared.Store.StoreSortInfo;
import com.sencha.gxt.data.shared.TreeStore;
import com.sencha.gxt.data.shared.loader.ChildTreeStoreBinding;
import com.sencha.gxt.data.shared.loader.TreeLoader;
import com.sencha.gxt.widget.core.client.Component;
import com.sencha.gxt.widget.core.client.event.CheckChangedEvent;
import com.sencha.gxt.widget.core.client.event.CheckChangedEvent.CheckChangedHandler;
import com.sencha.gxt.widget.core.client.selection.SelectionChangedEvent;
import com.sencha.gxt.widget.core.client.selection.SelectionChangedEvent.SelectionChangedHandler;
import com.sencha.gxt.widget.core.client.tree.Tree;
import com.sencha.gxt.widget.core.client.tree.TreeSelectionModel;
import org.whirlplatform.component.client.BuilderManager;
import org.whirlplatform.component.client.ComponentBuilder;
import org.whirlplatform.component.client.Containable;
import org.whirlplatform.component.client.event.ClickEvent;
import org.whirlplatform.component.client.event.EventManager;
import org.whirlplatform.component.client.event.SelectEvent;
import org.whirlplatform.component.client.ext.XTree;
import org.whirlplatform.component.client.utils.InfoHelper;
import org.whirlplatform.meta.shared.ClassMetadata;
import org.whirlplatform.meta.shared.EventMetadata;
import org.whirlplatform.meta.shared.FieldMetadata;
import org.whirlplatform.meta.shared.component.ComponentType;
import org.whirlplatform.meta.shared.component.PropertyType;
import org.whirlplatform.meta.shared.data.DataType;
import org.whirlplatform.meta.shared.data.DataValue;
import org.whirlplatform.meta.shared.data.RowModelData;
import org.whirlplatform.meta.shared.data.RowModelDataImpl;
import org.whirlplatform.rpc.client.DataServiceAsync;
import org.whirlplatform.rpc.shared.SessionToken;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TreeMenuBuilder extends TreeBuilder implements ClickEvent.HasClickHandlers, Containable {

    // TODO переписать, выкинуть наследование от TreeBuilder

    private String eventColumn;
    private XTree<RowModelData, String> tree;
    private HandlerRegistration registration;
    private List<ComponentBuilder> children;
    private Map<ComponentBuilder, RowModelData> builderMap;
    private IconProvider<RowModelData> iconProvider;

    public TreeMenuBuilder(Map<String, DataValue> builderProperties) {
        super(builderProperties);
    }

    public TreeMenuBuilder() {
        super();
    }

    public ComponentType getType() {
        return ComponentType.TreeMenuType;
    }

    @Override
    protected Component init(Map<String, DataValue> builderProperties) {
        children = new ArrayList<ComponentBuilder>();
        builderMap = new HashMap<ComponentBuilder, RowModelData>();
        iconProvider = new IconProvider<RowModelData>() {
            @Override
            public ImageResource getIcon(RowModelData model) {
                // для потомков, добавленных вручную (MenuItemBuilder), можно
                // выставить иконку
                if (model.getId().startsWith("temp")) {
                    String image = model.get("image");
                    if (image != null && BuilderManager.getApplicationData() != null) {
                        String role = BuilderManager.getApplicationData().getApplicationCode();
                        SafeUri s = getImageUrl(role, image);
                        return IconHelper.getImageResource(s, 16, 16);
                    }
                }
                return null;
            }
        };

        return super.init(builderProperties);
    }

    @Override
    public boolean setProperty(String name, DataValue value) {
        boolean result = super.setProperty(name, value);
        if (name.equalsIgnoreCase(PropertyType.EventColumn.getCode())) {
            if (value != null) {
                eventColumn = value.getString();
                return true;
            }
        }
        return result;
    }

    @Override
    public void addChild(ComponentBuilder child) {
        if (child instanceof HorizontalMenuItemBuilder) {
            RowModelData root = getModelData((HorizontalMenuItemBuilder) child);

            children.add(child);
            builderMap.put(child, root);
            // buildTree((MenuItemBuilder) child);
            // loadLocalData();
            // TODO child.setParentBuilder(???)
        }
    }

    @Override
    public void removeChild(ComponentBuilder child) {
        builderMap.remove(child);
        children.remove(child);
    }

    @Override
    public void clearContainer() {
        // TODO Auto-generated method stub
    }

    @Override
    public void forceLayout() {
        // TODO Auto-generated method stub
    }

    @Override
    public ComponentBuilder[] getChildren() {
        return children.toArray(new ComponentBuilder[children.size()]);
    }

    @Override
    public int getChildrenCount() {
        return children.size();
    }

    @Override
    public HandlerRegistration addClickHandler(ClickEvent.ClickHandler handler) {
        return addHandler(handler, ClickEvent.getType());
    }

    @Override
    public HandlerRegistration addSelectHandler(SelectEvent.SelectHandler handler) {
        registration = addHandler(handler, SelectEvent.getType());
        return registration;
    }

    protected TreeLoader<RowModelData> initLoader(final TreeStore<RowModelData> store) {
        RpcProxy<RowModelData, List<RowModelData>> proxy = createProxy();

        TreeLoader<RowModelData> loader = new TreeLoader<RowModelData>(proxy) {
            @Override
            public boolean hasChildren(RowModelData parent) {
                // если DataSource не установлен, то используются только
                // локальные данные (MenuItemBuilder)
                if (getClassMetadata().getClassId() != null && parent.getProperties().containsKey(isLeafColumn)) {
                    return Boolean.valueOf(parent.<Boolean>get(isLeafColumn));
                } else {
                    HorizontalMenuItemBuilder cb = (HorizontalMenuItemBuilder) findBuilder(parent);
                    return cb != null && cb.getChildren().length > 0;
                }
            }

            @Override
            protected void onLoadSuccess(RowModelData loadConfig, List<RowModelData> result) {
                super.onLoadSuccess(loadConfig, result);

                loadLocalData();
                tree.unmask();
                restoreState(result);
            }

            @Override
            protected void onLoadFailure(RowModelData loadConfig, Throwable t) {
                super.onLoadFailure(loadConfig, t);

                loadLocalData();
                tree.unmask();
            }

            @Override
            protected void loadData(RowModelData config) {
                if (builderMap.values().contains(config)) {
                    // добавляем в результат loader-а локальные данные
                    // (MenuItemBuilder)
                    ComponentBuilder builder = findBuilder(config);
                    List<RowModelData> result = new ArrayList<RowModelData>();
                    if (builder != null) {
                        for (ComponentBuilder c : ((HorizontalMenuItemBuilder) builder).getChildren()) {
                            RowModelData m = builderMap.get(c);
                            if (m == null) {
                                m = getModelData((HorizontalMenuItemBuilder) c);
                                builderMap.put(c, m);
                            }
                            result.add(m);
                        }
                    }
                    onLoadSuccess(config, result);
                } else {
                    super.loadData(config);
                }
            }
        };
        loader.addLoadHandler(new ChildTreeStoreBinding<RowModelData>(store));
        return loader;
    }

    @Override
    protected XTree<RowModelData, String> initTree(TreeLoader<RowModelData> loader) {
        tree = super.initTree(loader);

        // Чтобы событие вызывалось не только при смене выбранного элемента, но
        // и при клике
        tree.setSelectionModel(new TreeSelectionModel<RowModelData>() {
            @Override
            protected void onMouseClick(com.google.gwt.event.dom.client.ClickEvent ce) {
                fireSelectionChangeOnClick = true;
                super.onMouseClick(ce);
            }
        });

        tree.setIconProvider(iconProvider);

        if (checkChangedHandler != null) {
            checkChangedHandler.removeHandler();
        }
        checkChangedHandler = tree.addCheckChangedHandler(new CheckChangedHandler<RowModelData>() {
            @Override
            public void onCheckChanged(CheckChangedEvent<RowModelData> event) {
                tree.fireEvent(new SelectEvent());
            }
        });

        tree.getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler<RowModelData>() {

            @Override
            public void onSelectionChanged(SelectionChangedEvent<RowModelData> event) {
                for (Entry<ComponentBuilder, RowModelData> entry : builderMap.entrySet()) {
                    if (event.getSelection().size() > 0 && entry.getValue() == event.getSelection().get(0)) {
                        entry.getKey().fireEvent(new ClickEvent());
                        break;
                    }
                }
            }
        });

        tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        SimpleSafeHtmlCell<String> cell = new SimpleSafeHtmlCell<String>(SimpleSafeHtmlRenderer.getInstance(),
                "click") {
            @Override
            public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
                                       ValueUpdater<String> valueUpdater) {
                super.onBrowserEvent(context, parent, value, event, valueUpdater);
                if (eventColumn != null && "click".equals(event.getType())) {
                    if (registration != null) {
                        registration.removeHandler();
                    }

                    RowModelData model = getStore().getChild(context.getIndex());
                    String eventCode = model.get(eventColumn);

                    ComponentBuilder cb = findBuilder(model);

                    if (cb != null) {
                        cb.fireEvent(new ClickEvent());
                    } else {
                        loadEvent(eventCode);
                    }
                }
            }
        };
        tree.setCell(cell);

        Comparator<RowModelData> comp = new Comparator<RowModelData>() {

            // У записей из базы индекс null, они идут после добавленных вручную
            @Override
            public int compare(RowModelData o1, RowModelData o2) {
                Object value1 = o1.get(PropertyType.LayoutDataIndex.getCode());
                Object value2 = o2.get(PropertyType.LayoutDataIndex.getCode());
                if (value1 == null) {
                    return 1;
                } else if (value2 == null) {
                    return -1;
                } else {
                    return ((Double) value1).intValue() - ((Double) value2).intValue();
                }
            }
        };
        tree.getStore().addSortInfo(new StoreSortInfo(comp, SortDir.ASC));
        return tree;
    }

    @Override
    protected ClassMetadata getClassMetadata() {
        ClassMetadata metadata = super.getClassMetadata();

        if (eventColumn != null) {
            metadata.addField(new FieldMetadata(eventColumn, DataType.STRING, null));
        }
        return metadata;
    }

    private ComponentBuilder findBuilder(RowModelData model) {
        ComponentBuilder builder = null;
        for (Entry<ComponentBuilder, RowModelData> cb : builderMap.entrySet()) {
            if (cb.getValue().equals(model)) {
                builder = cb.getKey();
                break;
            }
        }
        return builder;
    }

    private SafeUri getImageUrl(String role, String image) {
        StringBuilder url = new StringBuilder();
        url.append(GWT.getHostPageBaseURL());
        url.append("resource?type=download&data=image&code=");
        url.append(role);
        url.append("&fileName=");
        url.append(image);
        return UriUtils.fromString(url.toString());
    }

    private RowModelData getModelData(HorizontalMenuItemBuilder cb) {
        RowModelData model = new RowModelDataImpl();
        model.setId("temp" + Random.nextInt());
        // if (nameExpression == null) {
        // nameExpression = "labelColumn";
        // }
        model.set(labelColumn, cb.getTitle());
        model.set("image", cb.getImage());
        model.set(PropertyType.LayoutDataIndex.getCode(), cb.getIndexPosition());
        return model;
    }

    private void buildTree(HorizontalMenuItemBuilder item) {
        RowModelData root = builderMap.get(item);
        for (ComponentBuilder cb : item.getChildren()) {
            RowModelData model = getModelData((HorizontalMenuItemBuilder) cb);
            builderMap.put(cb, model);
            store.add(root, model);

            if (((HorizontalMenuItemBuilder) cb).getChildren().length > 0) {
                buildTree((HorizontalMenuItemBuilder) cb);
            }
        }
    }

    private void loadEvent(String eventCode) {
        AsyncCallback<EventMetadata> callback = new AsyncCallback<EventMetadata>() {

            @Override
            public void onFailure(Throwable caught) {
                InfoHelper.throwInfo("get-event", caught);
            }

            @Override
            public void onSuccess(EventMetadata result) {
                EventManager.Util.get().addMenuTreeEvent(TreeMenuBuilder.this, result);
                fireEvent(new SelectEvent());
            }
        };
        DataServiceAsync.Util.getDataService(callback).getEvent(SessionToken.get(), eventCode);
    }

    private void loadLocalData() {
        for (int i = 0; i < children.size(); i++) {
            ComponentBuilder cb = children.get(i);
            RowModelData model = builderMap.get(cb);
            if (store.findModel(model) == null) {
                store.insert(i, model);
                buildTree((HorizontalMenuItemBuilder) cb);
            }
        }
    }

    public void showLocal() {
        loadLocalData();
        tree.unmask();
    }

    public Tree<RowModelData, String> getTree() {
        return tree;
    }
}
