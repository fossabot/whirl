package org.whirlplatform.component.client.hotkey;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.sencha.gxt.core.client.util.Util;
import com.sencha.gxt.widget.core.client.Component;
import com.sencha.gxt.widget.core.client.container.FlowLayoutContainer;
import org.whirlplatform.component.client.ComponentBuilder;
import org.whirlplatform.component.client.event.KeyPressEvent;
import org.whirlplatform.component.client.event.KeyPressEvent.HasKeyPressHandlers;
import org.whirlplatform.component.client.event.KeyPressEvent.KeyPressHandler;
import org.whirlplatform.component.client.utils.KeyMappings;
import org.whirlplatform.meta.shared.component.ComponentType;
import org.whirlplatform.meta.shared.component.PropertyType;
import org.whirlplatform.meta.shared.data.DataValue;

import java.util.Map;

public class HotKeyBuilder extends ComponentBuilder implements
		HasKeyPressHandlers {

	private boolean isAlt;
	private boolean isCtrl;
	private boolean isShift;

	private Integer keyCode;

	private HotKeyHandler handler;

	private FlowLayoutContainer container;

	public HotKeyBuilder(Map<String, DataValue> builderProperties) {
		super(builderProperties);
	}

	public HotKeyBuilder() {
		super();
	}

	@Override
	protected Component init(Map<String, DataValue> builderProperties) {
		isAlt = false;
		isCtrl = false;
		isShift = false;

		container = new FlowLayoutContainer();
		return container;
	}

	@Override
	public Component create() {
		Component component = super.create();
		if (keyCode != null) {
			handler = new HotKeyHandler(keyCode, this);
			handler.setAlt(isAlt);
			handler.setCtrl(isCtrl);
			handler.setShift(isShift);
			component.addAttachHandler(new Handler() {

				@Override
				public void onAttachOrDetach(AttachEvent event) {
					if (event.isAttached()) {
						HotKeyManager.get().addHotKeyHandler(handler);
					} else {
						HotKeyManager.get().removeHotKeyHandler(handler);
					}
				}

			});
		}
		return component;
	}

	@Override
	public boolean setProperty(String name, DataValue value) {
		if (name.equalsIgnoreCase(PropertyType.Alt.getCode())) {
			if (value != null){
				isAlt = Boolean.TRUE.equals(value.getBoolean());
				return true;
			}
		} else if (name.equalsIgnoreCase(PropertyType.Shift.getCode())) {
			if (value != null){
				isShift = Boolean.TRUE.equals(value.getBoolean());
				return true;
			}
		} else if (name.equalsIgnoreCase(PropertyType.Ctrl.getCode())) {
			if (value != null){
				isCtrl = Boolean.TRUE.equals(value.getBoolean());
				return true;
			}
		} else if (name.equalsIgnoreCase(PropertyType.Key.getCode())) {
			if (value != null){
			    String key = value.getString();
				if (!Util.isEmptyString(key)) {
					keyCode = KeyMappings.keyCode(key.trim());
				}
				return true;
			}
		}
		return super.setProperty(name, value);
	}

	@Override
	protected <C> C getRealComponent() {
		return (C) container;
	}

	@Override
	public ComponentType getType() {
		return ComponentType.HotKeyType;
	}

	public void setIcon(final ImageResource icon) {
		final Image image = new Image(icon);
		container.add(image);
	}

	@Override
	public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
		return ensureHandler().addHandler(KeyPressEvent.getType(), handler);
	}
}
