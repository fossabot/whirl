package org.whirlplatform.component.client.combo;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.sencha.gxt.cell.core.client.form.CheckBoxCell;
import com.sencha.gxt.cell.core.client.form.ComboBoxCell;
import com.sencha.gxt.cell.core.client.form.TriggerFieldCell.TriggerFieldAppearance;
import com.sencha.gxt.core.client.Style.SelectionMode;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.core.client.dom.DomQuery;
import com.sencha.gxt.core.client.util.Util;
import com.sencha.gxt.data.shared.SortDir;
import com.sencha.gxt.data.shared.Store.StoreSortInfo;
import com.sencha.gxt.data.shared.loader.LoadEvent;
import com.sencha.gxt.data.shared.loader.LoadHandler;
import com.sencha.gxt.theme.base.client.field.TriggerFieldDefaultAppearance;
import com.sencha.gxt.widget.core.client.Component;
import com.sencha.gxt.widget.core.client.ListView;
import com.sencha.gxt.widget.core.client.event.BeforeQueryEvent;
import com.sencha.gxt.widget.core.client.event.BeforeQueryEvent.BeforeQueryHandler;
import com.sencha.gxt.widget.core.client.event.TriggerClickEvent;
import com.sencha.gxt.widget.core.client.event.TriggerClickEvent.TriggerClickHandler;
import com.sencha.gxt.widget.core.client.form.ComboBox;
import org.whirlplatform.component.client.ListParameter;
import org.whirlplatform.component.client.data.ClassStore;
import org.whirlplatform.component.client.ext.CountElement;
import org.whirlplatform.component.client.selenium.Locator;
import org.whirlplatform.component.client.utils.CSVParser;
import org.whirlplatform.meta.shared.ClassLoadConfig;
import org.whirlplatform.meta.shared.LoadData;
import org.whirlplatform.meta.shared.component.ComponentType;
import org.whirlplatform.meta.shared.component.PropertyType;
import org.whirlplatform.meta.shared.data.DataValue;
import org.whirlplatform.meta.shared.data.ListModelData;
import org.whirlplatform.meta.shared.data.ListModelDataImpl;
import org.whirlplatform.meta.shared.data.RowListValue;
import org.whirlplatform.meta.shared.data.RowListValueImpl;
import org.whirlplatform.meta.shared.data.RowValue;
import org.whirlplatform.meta.shared.data.RowValueImpl;
import org.whirlplatform.meta.shared.i18n.AppMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiComboBoxBuilder<T extends ComboBox<ListModelData>> extends ComboBoxBuilder<T> implements
        ListParameter<RowListValue> {

    protected CountElement ce;
    private Map<ListModelData, Boolean> modelCheck;
    protected ListView<ListModelData, ?> listView;

    class CheckedModels {
        List<ListModelData> models = new ArrayList<>();
        boolean idReady = false;
        boolean labelReady = false;

        boolean isReady() {
            return idReady && labelReady;
        }
    }

    protected CheckedModels checkedModels;

    protected boolean singleSelection;

    protected boolean readOnly;

    private ValueProvider<ListModelData, Boolean> valueProvider;

    private CheckBoxCell cell;

    public MultiComboBoxBuilder(Map<String, DataValue> builderProperties) {
        super(builderProperties);
    }

    public MultiComboBoxBuilder() {
        super();
    }

    @Override
    public ComponentType getType() {
        return ComponentType.MultiComboBoxType;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component init(Map<String, DataValue> builderProperties) {
        required = false;

        minChars = 2;
        delayTimeMs = 1200;
        saveState = false;
        restoreState = false;
        singleSelection = false;

        initParamHelper();
        initLabelProvider();

        modelCheck = new HashMap<ListModelData, Boolean>();
        checkedModels = new CheckedModels();
        valueProvider = new ValueProvider<ListModelData, Boolean>() {

            @Override
            public Boolean getValue(ListModelData object) {
                return modelCheck.get(object);
            }

            @Override
            public void setValue(ListModelData object, Boolean value) {
                modelCheck.put(object, value);
            }

            @Override
            public String getPath() {
                return null;
            }

        };

        cell = new CheckBoxCell() {

            private ListModelData cellModel;

            @Override
            public void render(Context context, Boolean value, SafeHtmlBuilder sb) {
                ListModelData m = getStore().findModelWithKey((String) context.getKey());
                Boolean v = false;
                if (checkedModels.models.contains(m)) {
                    v = true;
                }
                cellModel = m;
                super.render(context, v, sb);
                cellModel = null;
            }

            @Override
            public SafeHtml getBoxLabel() {
                if (cellModel != null) {
                    return SafeHtmlUtils.fromTrustedString(cellModel.getLabel());
                }
                return super.getBoxLabel();
            }

            @Override
            public void onBrowserEvent(
                    com.google.gwt.cell.client.Cell.Context context,
                    Element parent, Boolean value, NativeEvent event,
                    ValueUpdater<Boolean> valueUpdater) {
                if ("change".equalsIgnoreCase(event.getType())) {
                    int idx = listView.findElementIndex(parent);
                    if (readOnly) {
                        Element e = listView.getElement(idx);
                        getInputElement(e).setChecked(false);
                        return;
                    }
                    ListModelData model = listView.getStore().get(idx);
                    if (model.getId() == null) {
                        return;
                    }

                    if (singleSelection) {
                        checkedModels.models.clear();
                        for (Element e : listView.getElements()) {
                            if (getInputElement(e) != getInputElement(parent)) {
                                getInputElement(e).setChecked(false);
                            }
                        }
                    }

                    if (!getInputElement(parent).isChecked()) {
                        checkedModels.models.remove(model);
                    } else {
                        checkedModels.models.add(model);
                    }
                    ce.setCount(checkedModels.models.size());
                    saveState();
                    ValueChangeEvent.fire(comboBox, model);
                }
            }
        };
        listView = new ListView<ListModelData, Boolean>(null, valueProvider, cell);
        listView.getSelectionModel().setSelectionMode(SelectionMode.SIMPLE);
        comboBox = (T) new ComboBox<ListModelData>(new ComboBoxCell<ListModelData>(null, labelProvider, listView) {
            @Override
            protected void onSelect(ListModelData item) {
            }
        });
        comboBox.setQueryDelay(delayTimeMs);
        initCountElement();
        return comboBox;
    }

    protected void initCountElement() {
        ce = new CountElement(DomQuery.selectNode("input",
                comboBox.getElement()));
        ce.setCount(0);
    }

    @Override
    public Component create() {
        Component c = super.create();
        calcSingleSelection();
        calcCounter();
        return c;
    }

    @Override
    protected void bindStore() {
        final ClassStore<ListModelData, ClassLoadConfig> store = getStore();
        if (!(comboBox.getStore() != store || comboBox.getListView().getStore() != store)) {
            return;
        }

        // Чтобы отмеченные элементы отображались в начале списка
        StoreSortInfo<ListModelData> sortInfo = new StoreSortInfo<ListModelData>(
                new Comparator<ListModelData>() {
                    @Override
                    public int compare(ListModelData o1, ListModelData o2) {
                        if (checkedModels.models.contains(o1)
                                && !checkedModels.models.contains(o2)) {
                            return -1;
                        } else if (!checkedModels.models.contains(o1)
                                && checkedModels.models.contains(o2)) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }, SortDir.ASC);
        store.addSortInfo(sortInfo);
        store.getLoader().addLoadHandler(
                new LoadHandler<ClassLoadConfig, LoadData<ListModelData>>() {

                    @Override
                    public void onLoad(
                            LoadEvent<ClassLoadConfig, LoadData<ListModelData>> event) {
                        // Если в комбобокс не введено значение, добавляем
                        // отмеченные элементы
                        if (event.getLoadConfig().getQuery() == null
                                || event.getLoadConfig().getQuery().isEmpty()) {
                            for (ListModelData m : checkedModels.models) {
                                if (!store.getAll().contains(m)) {
                                    store.add(m);
                                }
                            }
                        }
                        // Чтобы в комбобоксе не выделялся текст при загрузке
                        // данных
                        comboBox.select(comboBox.getText().length(), 0);
                    }
                });
        comboBox.setStore(store);
    }

    @Override
    protected void addListener() {
        comboBox.addTriggerClickHandler(new TriggerClickHandler() {

            @Override
            public void onTriggerClick(TriggerClickEvent event) {
                if (!comboBox.isExpanded()) {
                    getStore().clear();
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                        @Override
                        public void execute() {
                            load();
                        }
                    });
                }
            }
        });

        comboBox.addBeforeQueryHandler(new BeforeQueryHandler<ListModelData>() {
            @Override
            public void onBeforeQuery(BeforeQueryEvent<ListModelData> event) {
                if (event.getQuery().length() >= minChars) {
                    load(false);
                    event.setCancelled(true);
                }
            }
        });

        comboBox.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                comboBox.setMinListWidth((int) (comboBox.getOffsetWidth() * 1.3));
            }
        });
    }

    @Override
    public void setValue(ListModelData value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListModelData getValue() {
        throw new UnsupportedOperationException();
    }

    public String getText() {
        return comboBox.getText();
    }

    @Override
    public void clear() {
        checkedModels.models.clear();
        calcCounter();
        saveState();
        ValueChangeEvent.fire(getRealComponent(), null);
    }

    @Override
    public boolean isValid(boolean invalidate) {
        if (isRequired() && checkedModels.models.size() == 0) {
            if (invalidate) {
                comboBox.markInvalid(AppMessage.Util.MESSAGE.requiredField());
            }
            return false;
        }
        if (invalidate) {
            comboBox.clearInvalid();
        }
        return true;
    }

    @Override
    public RowListValue getFieldValue() {
        RowListValue result = new RowListValueImpl();
        result.setCode(getCode());
        for (ListModelData model : checkedModels.models) {
            RowValue row = new RowValueImpl(model.getId());
            row.setChecked(true);
            result.addRowValue(row);
        }
        return result;
    }

    @Override
    public void setFieldValue(RowListValue value) {
        int size = checkedModels.models.size();
        checkedModels.models.clear();
        ListModelData m = null;
        if (value != null) {
            for (RowValue r : value.getRowList()) {
                m = new ListModelDataImpl();
                m.setId(r.getId());
                m.setLabel(r.getLabel());
                checkedModels.models.add(m);
            }
        }
        calcCounter();

        // Если хоть одна запись была добавлена либо удалена, оповещаем слушателей
        if (m != null || size > 0) {
            ValueChangeEvent.fire(comboBox, m);
        }
    }

    private void calcCounter() {
        ce.setCount(checkedModels.models.size());
        if (clearDecorator == null) {
            return;
        }
        if (checkedModels.models.size() > 0) {
            clearDecorator.show();
        } else {
            clearDecorator.hide();
        }
    }

    @Override
    public boolean setProperty(String name, DataValue value) {
        if (name.equalsIgnoreCase(PropertyType.Cleanable.getCode())) {
            if (value.getBoolean()) {
                super.setProperty(name, value);
                setClearCrossRightOffset(35);
            }
            return true;
        } else if (name.equalsIgnoreCase(PropertyType.StringValue.getCode())) {
            parseValue(value.getString(), false);
            return true;
        } else if (name.equalsIgnoreCase(PropertyType.DisplayValue.getCode())) {
            parseValue(value.getString(), true);
            return true;
        } else if (name
                .equalsIgnoreCase(PropertyType.SingleSelection.getCode())) {
            singleSelection = value.getBoolean();
            return true;
        }
        return super.setProperty(name, value);
    }

    protected void parseValue(String value, boolean labels) {
        if (Util.isEmptyString(value)) {
            checkedModels.models.clear();
            return;
        }
        try {
            CSVParser parser = new CSVParser();
            String[] array = parser.parseLine(value);
            for (int i = 0; i < array.length; i++) {
                ListModelData model;
                if (checkedModels.models.size() < i + 1) {
                    model = new ListModelDataImpl();
                    checkedModels.models.add(model);
                } else {
                    model = checkedModels.models.get(i);
                }
                if (labels) {
                    model.setLabel(array[i]);
                } else {
                    model.setId(array[i]);
                }

            }
            if (labels) {
                checkedModels.labelReady = true;
            } else {
                checkedModels.idReady = true;
            }
        } catch (IOException e) {
        }
    }

    private void calcSingleSelection() {
        if (singleSelection && checkedModels.models.size() > 1) {
            ListModelData model = checkedModels.models.get(checkedModels.models.size() - 1);
            checkedModels.models.clear();
            checkedModels.models.add(model);
        }
    }

    @Override
    protected void restoreSelectionState() {
        if (restoreState) {
            RowListValue v = (RowListValue) getSelectionStore().restore(
                    getId() + "/select");
            setFieldValue(v);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private static class LocatorParams {
        private static String TYPE_INPUT = "Input";
        private static String TYPE_TRIGGER = "Trigger";
        private static String TYPE_CLEAR = "Clear";
        private static String TYPE_ITEM = "Item";
        private static String TYPE_CHECK = "Check";
        private static String PARAMETER_ID = "id";
        private static String PARAMETER_INDEX = "index";
        private static String PARAMETER_LABEL = "label";
    }

    // TODO Selenium

    @Override
    public Locator getLocatorByElement(Element element) {
        Locator part = null;
        Locator locator = getLocator(element);
        if (locator != null) {
            if (comboBox.getCell().getInputElement(comboBox.getElement()).isOrHasChild(element)) {
                part = new Locator(LocatorParams.TYPE_INPUT);
            }
            if (comboBox.getCell().getAppearance().triggerIsOrHasChild(comboBox.getElement(), element)) {
                part = new Locator(LocatorParams.TYPE_TRIGGER);
            }
            if (super.clearDecorator != null) {
                Element clearElement = super.clearDecorator.getElement();
                if (clearElement != null && clearElement.isOrHasChild(element)) {
                    part = new Locator(LocatorParams.TYPE_CLEAR);
                }
            }
        } else {
            List<Element> list = listView.getElements();
            for (int idx = 0; idx < list.size(); idx++) {
                Element item = list.get(idx);
                if (item.isOrHasChild(element)) {
                    locator = getLocator(comboBox.getElement());
                    ListModelData d = comboBox.getStore().get(idx);
                    part = new Locator(LocatorParams.TYPE_ITEM);
                    part.setParameter(LocatorParams.PARAMETER_ID, d.getId());
                    part.setParameter(LocatorParams.PARAMETER_LABEL, d.getLabel());
                    part.setParameter(LocatorParams.PARAMETER_INDEX, String.valueOf(idx));
                    if (cell.getInputElement(item).isOrHasChild(element)) {
                        part.setPart(new Locator(LocatorParams.TYPE_CHECK));
                    }
                    break;
                }
            }
        }
        if (locator != null && part != null) {
            locator.setPart(part);
        }
        return locator;
    }


    @Override
    public Element getElementByLocator(Locator locator) {
        Element element = null;
        if (!super.fitsLocator(locator)) {
            return null;
        }
        Locator part = locator.getPart();
        if (part != null) {
            if (LocatorParams.TYPE_INPUT.equals(part.getType())) {
                element = comboBox.getCell().getInputElement(comboBox.getElement());
            }
            if (LocatorParams.TYPE_TRIGGER.equals(part.getType())) {
                TriggerFieldAppearance appearance = comboBox.getCell().getAppearance();
                if (appearance instanceof TriggerFieldDefaultAppearance) {
                    TriggerFieldDefaultAppearance defApp = (TriggerFieldDefaultAppearance) appearance;
                    element = comboBox.getElement().selectNode("." + defApp.getStyle().trigger()); // TODO надо что-то сделать
                }
            }
            if (LocatorParams.TYPE_CLEAR.equals(part.getType())) {
                if (super.clearDecorator != null) {
                    element = super.clearDecorator.getElement();
                }
            }
            // Проверяем что комбобокс раскрылся.
            // Только тогда следует возвращать элементы.
            if (LocatorParams.TYPE_ITEM.equals(part.getType()) && comboBox.isExpanded()) {
                if (part.hasParameter(LocatorParams.PARAMETER_ID)) {
                    String id = part.getParameter(LocatorParams.PARAMETER_ID);
                    int index = store.indexOf(store.findModelWithKey(id));
                    element = listView.getElement(index);
                } else if (part.hasParameter(LocatorParams.PARAMETER_INDEX)) {
                    int index = Integer.parseInt(part.getParameter(LocatorParams.PARAMETER_INDEX));
                    element = listView.getElement(index);
                } else if (part.hasParameter(LocatorParams.PARAMETER_LABEL)
                        && Util.isEmptyString(part.getParameter(LocatorParams.PARAMETER_LABEL))) {
                    String label = part.getParameter(LocatorParams.PARAMETER_LABEL);
                    for (ListModelData m : store.getAll()) {
                        if (label.equals(m.getLabel())) {
                            element = listView.getElement(store.indexOf(m));
                            break;
                        }
                    }
                }
                Locator partPart = part.getPart();
                if (partPart != null && LocatorParams.TYPE_CHECK.equals(partPart.getType())) {
                    element = cell.getInputElement(element);
                }
            }
        }
        return element;
    }
}
