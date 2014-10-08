/*
 * HomePane.java 15 mai 2006
 *
 * Copyright (c) 2006 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * The MVC view that edits a home. 
 * @author Emmanuel Puybaret
 */
public class HomePane extends JRootPane {
  public enum ActionType {
    NEW_HOME, CLOSE, OPEN, SAVE, SAVE_AS, PREFERENCES, EXIT, 
    UNDO, REDO, CUT, COPY, PASTE, DELETE, SELECT_ALL,
    ADD_HOME_FURNITURE, DELETE_HOME_FURNITURE, MODIFY_HOME_FURNITURE,
    SORT_HOME_FURNITURE_BY_NAME, SORT_HOME_FURNITURE_BY_WIDTH, SORT_HOME_FURNITURE_BY_DEPTH, SORT_HOME_FURNITURE_BY_HEIGHT, 
    SORT_HOME_FURNITURE_BY_COLOR, SORT_HOME_FURNITURE_BY_MOVABILITY, SORT_HOME_FURNITURE_BY_TYPE, SORT_HOME_FURNITURE_BY_VISIBILITY, 
    SORT_HOME_FURNITURE_BY_DESCENDING_ORDER,
    ALIGN_FURNITURE_ON_TOP, ALIGN_FURNITURE_ON_BOTTOM, ALIGN_FURNITURE_ON_LEFT, ALIGN_FURNITURE_ON_RIGHT,
    WALL_CREATION, DELETE_SELECTION, MODIFY_WALL, ZOOM_OUT, ZOOM_IN,  
    ABOUT}
  public enum SaveAnswer {SAVE, CANCEL, DO_NOT_SAVE}

  private static final String SWEET_HOME_3D_EXTENSION = ".sh3d";
  private static final FileFilter SWEET_HOME_3D_FILTER = new FileFilter() {
      @Override
      public boolean accept(File file) {
        // Accept directories and .sh3d files
        return file.isDirectory()
               || file.getName().toLowerCase().
                      endsWith(SWEET_HOME_3D_EXTENSION);
      }
  
      @Override
      public String getDescription() {
        return "Sweet Home 3D";
      }
    }; 
  private static File currentDirectory;

  private ResourceBundle                  resource;
  // Button model shared by Wall creation menu item and the matching tool bar button
  private JToggleButton.ToggleButtonModel wallCreationToggleModel;
  private JComponent                      focusedComponent;
  private JComponent                      catalogView;
  private JComponent                      furnitureView;
  private JComponent                      planView;
  private TransferHandler                 catalogTransferHandler;
  private TransferHandler                 furnitureTransferHandler;
  private TransferHandler                 planTransferHandler;
  
  /**
   * Creates this view associated with its controller.
   */
  public HomePane(Home home, UserPreferences preferences, HomeController controller) {
    this.resource = ResourceBundle.getBundle(HomePane.class.getName());
    // Create a unique toggle button model for Wall creation / Selection states
    // so Wall creation menu item and tool bar button 
    // always reflect the same toggle state at screen
    this.wallCreationToggleModel = new JToggleButton.ToggleButtonModel();
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    
    createActions(controller);
    createTransferHandlers(home, preferences, controller);
    JMenuBar homeMenuBar = getHomeMenuBar(home);
    setJMenuBar(homeMenuBar);
    getContentPane().add(getToolBar(), BorderLayout.NORTH);
    getContentPane().add(getMainPane(home, preferences, controller));
    
    disableMenuItemsDuringDragAndDrop(this.planView, homeMenuBar);
  }
  
  /**
   * Create the actions map of this component.
   */
  private void createActions(final HomeController controller) {
    createAction(ActionType.NEW_HOME, controller, "newHome");
    createAction(ActionType.OPEN, controller, "open");
    createAction(ActionType.CLOSE, controller, "close");
    createAction(ActionType.SAVE, controller, "save");
    createAction(ActionType.SAVE_AS, controller, "saveAs");
    createAction(ActionType.PREFERENCES, controller, "editPreferences");
    createAction(ActionType.EXIT, controller, "exit");
    
    createAction(ActionType.UNDO, controller, "undo");
    createAction(ActionType.REDO, controller, "redo");
    createClipboardAction(ActionType.CUT, TransferHandler.getCutAction());
    createClipboardAction(ActionType.COPY, TransferHandler.getCopyAction());
    createClipboardAction(ActionType.PASTE, TransferHandler.getPasteAction());
    createAction(ActionType.DELETE, controller, "delete");
    createAction(ActionType.SELECT_ALL, controller, "selectAll");
    
    createAction(ActionType.ADD_HOME_FURNITURE, controller, "addHomeFurniture");
    createAction(ActionType.DELETE_HOME_FURNITURE,
        controller.getFurnitureController(), "deleteSelection");
    createAction(ActionType.MODIFY_HOME_FURNITURE,
        controller.getFurnitureController(), "modifySelectedFurniture");
    createAction(ActionType.ALIGN_FURNITURE_ON_TOP, 
        controller.getFurnitureController(), "alignSelectedFurnitureOnTop");
    createAction(ActionType.ALIGN_FURNITURE_ON_BOTTOM, 
        controller.getFurnitureController(), "alignSelectedFurnitureOnBottom");
    createAction(ActionType.ALIGN_FURNITURE_ON_LEFT, 
        controller.getFurnitureController(), "alignSelectedFurnitureOnLeft");
    createAction(ActionType.ALIGN_FURNITURE_ON_RIGHT, 
        controller.getFurnitureController(), "alignSelectedFurnitureOnRight");
    createAction(ActionType.SORT_HOME_FURNITURE_BY_NAME, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.NAME);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_WIDTH, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.WIDTH);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_DEPTH, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.DEPTH);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_HEIGHT, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.HEIGHT);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_COLOR, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.COLOR);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_MOVABILITY, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.MOVABLE);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_TYPE, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.DOOR_OR_WINDOW);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_VISIBILITY, controller, "toggleFurnitureSort", 
        HomePieceOfFurniture.SortableProperty.VISIBLE);
    createAction(ActionType.SORT_HOME_FURNITURE_BY_DESCENDING_ORDER, controller, "toggleFurnitureSortOrder");
    
    getActionMap().put(ActionType.WALL_CREATION,
        new ResourceAction (this.resource, ActionType.WALL_CREATION.toString()) {
          public void actionPerformed(ActionEvent ev) {
            boolean selected = ((AbstractButton)ev.getSource()).isSelected();
            if (selected) {
              controller.setWallCreationMode();
            } else {
              controller.setSelectionMode();
            }
          }
        });
    createAction(ActionType.DELETE_SELECTION, 
        controller.getPlanController(), "deleteSelection");
    createAction(ActionType.MODIFY_WALL, 
        controller.getPlanController(), "modifySelectedWalls");
    createAction(ActionType.ZOOM_OUT, controller, "zoomOut");
    createAction(ActionType.ZOOM_IN, controller, "zoomIn");
    createAction(ActionType.ABOUT, controller, "about");
  }

  /**
   * Creates a <code>ControllerAction</code> object that calls on <code>controller</code> a given
   * <code>method</code> with its <code>parameters</code>.
   */
  private void createAction(ActionType action, Object controller, String method, Object ... parameters) {
    try {
      getActionMap().put(action, new ControllerAction(
          this.resource, action.toString(), controller, method, parameters));
    } catch (NoSuchMethodException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  /**
   * Creates a <code>ReourceAction</code> object that calls 
   * <code>actionPerfomed</code> method on a given 
   * existing <code>clipboardAction</code> with a source equal to focused component.
   */
  private void createClipboardAction(ActionType actionType, 
                                     final Action clipboardAction) {
    getActionMap().put(actionType,
        new ResourceAction (this.resource, actionType.toString()) {
          public void actionPerformed(ActionEvent ev) {
            ev = new ActionEvent(focusedComponent, ActionEvent.ACTION_PERFORMED, null);
            clipboardAction.actionPerformed(ev);
          }
        });
  }

  /**
   * Creates components transfer handlers.
   */
  private void createTransferHandlers(Home home, UserPreferences preferences, 
                                      HomeController controller) {
    this.catalogTransferHandler = 
        new CatalogTransferHandler(preferences.getCatalog());
    this.furnitureTransferHandler = 
        new FurnitureTransferHandler(home, controller);
    this.planTransferHandler = 
        new PlanTransferHandler(home, controller);
  }

  /**
   * Returns the menu bar displayed in this pane.
   */
  private JMenuBar getHomeMenuBar(final Home home) {
    // Create File menu
    JMenu fileMenu = new JMenu(new ResourceAction(this.resource, "FILE_MENU", true));
    fileMenu.add(getMenuAction(ActionType.NEW_HOME));
    fileMenu.add(getMenuAction(ActionType.OPEN));
    fileMenu.addSeparator();
    fileMenu.add(getMenuAction(ActionType.CLOSE));
    fileMenu.add(getMenuAction(ActionType.SAVE));
    fileMenu.add(getMenuAction(ActionType.SAVE_AS));
    // Don't add EXIT menu under Mac OS X, it's displayed in application menu  
    if (!System.getProperty("os.name").startsWith("Mac OS X")) {
      fileMenu.addSeparator();
      fileMenu.add(getMenuAction(ActionType.PREFERENCES));
      fileMenu.addSeparator();
      fileMenu.add(getMenuAction(ActionType.EXIT));
    }

    // Create Edit menu
    JMenu editMenu = new JMenu(new ResourceAction(this.resource, "EDIT_MENU", true));
    editMenu.add(getMenuAction(ActionType.UNDO));
    editMenu.add(getMenuAction(ActionType.REDO));
    editMenu.addSeparator();
    editMenu.add(getMenuAction(ActionType.CUT));
    editMenu.add(getMenuAction(ActionType.COPY));
    editMenu.add(getMenuAction(ActionType.PASTE));
    editMenu.addSeparator();
    editMenu.add(getMenuAction(ActionType.DELETE));
    editMenu.add(getMenuAction(ActionType.SELECT_ALL));

    // Create Furniture menu
    JMenu furnitureMenu = new JMenu(new ResourceAction(this.resource, "FURNITURE_MENU", true));
    furnitureMenu.add(getMenuAction(ActionType.ADD_HOME_FURNITURE));
    furnitureMenu.add(getMenuAction(ActionType.MODIFY_HOME_FURNITURE));
    furnitureMenu.addSeparator();
    furnitureMenu.add(getMenuAction(ActionType.ALIGN_FURNITURE_ON_TOP));
    furnitureMenu.add(getMenuAction(ActionType.ALIGN_FURNITURE_ON_BOTTOM));
    furnitureMenu.add(getMenuAction(ActionType.ALIGN_FURNITURE_ON_LEFT));
    furnitureMenu.add(getMenuAction(ActionType.ALIGN_FURNITURE_ON_RIGHT));
    furnitureMenu.addSeparator();
    // Create Furniture Sort submenu
    JMenu sortMenu = new JMenu(new ResourceAction(this.resource, "SORT_HOME_FURNITURE_MENU", true));
    // Map sort furniture properties to sort actions
    Map<String, Action> sortActions = new LinkedHashMap<String, Action>(); 
    sortActions.put("name", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_NAME)); 
    sortActions.put("width", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_WIDTH));
    sortActions.put("depth", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_DEPTH));
    sortActions.put("height", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_HEIGHT));
    sortActions.put("color", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_COLOR));
    sortActions.put("movable", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_MOVABILITY));
    sortActions.put("doorOrWindow", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_TYPE));
    sortActions.put("visible", getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_VISIBILITY));
    // Add radio button menu items to sub menu and make them share the same radio button group
    ButtonGroup sortButtonGroup = new ButtonGroup();
    for (Map.Entry<String, Action> entry : sortActions.entrySet()) {
      final String furnitureProperty = entry.getKey();
      Action sortAction = entry.getValue();
      JRadioButtonMenuItem sortMenuItem = new JRadioButtonMenuItem();
      // Use a special model for sort radio button menu item that is selected if
      // home is sorted on furnitureProperty criterion
      sortMenuItem.setModel(new JToggleButton.ToggleButtonModel() {
          @Override
          public boolean isSelected() {
            return furnitureProperty.equals(home.getFurnitureSortedProperty());
          }
        }); 
      // Configure check box menu item action after setting its model to avoid losing its mnemonic
      sortMenuItem.setAction(sortAction);
      sortMenu.add(sortMenuItem);
      sortButtonGroup.add(sortMenuItem);
    }
    sortMenu.addSeparator();
    JCheckBoxMenuItem sortOrderCheckBoxMenuItem = new JCheckBoxMenuItem();
    // Use a special model for sort order check box menu item that is selected depending on
    // home sort order property
    sortOrderCheckBoxMenuItem.setModel(new JToggleButton.ToggleButtonModel() {
        @Override
        public boolean isSelected() {
          return home.isFurnitureDescendingSorted();
        }
      });
    sortOrderCheckBoxMenuItem.setAction(
        getMenuAction(ActionType.SORT_HOME_FURNITURE_BY_DESCENDING_ORDER));
    sortMenu.add(sortOrderCheckBoxMenuItem);
    furnitureMenu.add(sortMenu);
    
    // Create Plan menu
    JMenu planMenu = new JMenu(new ResourceAction(this.resource, "PLAN_MENU", true));
    planMenu.add(getWallCreationCheckBoxMenuItem(false));
    planMenu.add(getMenuAction(ActionType.MODIFY_WALL));
    planMenu.addSeparator();
    planMenu.add(getMenuAction(ActionType.ZOOM_OUT));
    planMenu.add(getMenuAction(ActionType.ZOOM_IN));

    // Create Help menu
    JMenu helpMenu = null;
    if (!System.getProperty("os.name").startsWith("Mac OS X")) {
      helpMenu = new JMenu(
          new ResourceAction(this.resource, "HELP_MENU", true));
      helpMenu.add(getMenuAction(ActionType.ABOUT));      
    }

    // Add menus to menu bar
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(furnitureMenu);
    menuBar.add(planMenu);
    if (helpMenu != null) {
      menuBar.add(helpMenu);
    }

    return menuBar;
  }

  /**
   * Returns an action decorated for menu items.
   */
  private Action getMenuAction(ActionType actionType) {
    return new ResourceAction.MenuAction(getActionMap().get(actionType));
  }

  /**
   * Returns an action decorated for popup menu items.
   */
  private Action getPopupAction(ActionType actionType) {
    return new ResourceAction.PopupAction(getActionMap().get(actionType));
  }

  /**
   * Returns a check box menu item for wall creation action. 
   */
  private JCheckBoxMenuItem getWallCreationCheckBoxMenuItem(boolean popup) {
    JCheckBoxMenuItem wallCreationCheckBoxMenuItem = new JCheckBoxMenuItem();
    // Use the same model as Wall creation tool bar button
    wallCreationCheckBoxMenuItem.setModel(this.wallCreationToggleModel);
    // Configure check box menu item action after setting its model to avoid losing its mnemonic
    wallCreationCheckBoxMenuItem.setAction(
        popup ? getPopupAction(ActionType.WALL_CREATION)
              : getMenuAction(ActionType.WALL_CREATION));
    return wallCreationCheckBoxMenuItem;
  }
  
  /**
   * Returns the tool bar displayed in this pane.
   */
  private JToolBar getToolBar() {
    JToolBar toolBar = new JToolBar();
    ActionMap actions = getActionMap();    
    toolBar.add(actions.get(ActionType.NEW_HOME));
    toolBar.add(actions.get(ActionType.OPEN));
    toolBar.add(actions.get(ActionType.SAVE));
    toolBar.addSeparator();

    toolBar.add(actions.get(ActionType.UNDO));
    toolBar.add(actions.get(ActionType.REDO));
    toolBar.addSeparator();
    
    toolBar.add(actions.get(ActionType.CUT));
    toolBar.add(actions.get(ActionType.COPY));
    toolBar.add(actions.get(ActionType.PASTE));
    toolBar.addSeparator();
    
    toolBar.add(actions.get(ActionType.DELETE));
    toolBar.addSeparator();

    toolBar.add(actions.get(ActionType.ADD_HOME_FURNITURE));
    JToggleButton wallCreationToggleButton = 
      new JToggleButton(actions.get(ActionType.WALL_CREATION));
    // Use the same model as Wall creation menu item
    wallCreationToggleButton.setModel(this.wallCreationToggleModel);
    // Don't display text with icon
    wallCreationToggleButton.setText("");
    toolBar.add(wallCreationToggleButton);
    toolBar.addSeparator();
    
    toolBar.add(actions.get(ActionType.ALIGN_FURNITURE_ON_TOP));
    toolBar.add(actions.get(ActionType.ALIGN_FURNITURE_ON_BOTTOM));
    toolBar.add(actions.get(ActionType.ALIGN_FURNITURE_ON_LEFT));
    toolBar.add(actions.get(ActionType.ALIGN_FURNITURE_ON_RIGHT));
    toolBar.addSeparator();
   
    toolBar.add(actions.get(ActionType.ZOOM_OUT));
    toolBar.add(actions.get(ActionType.ZOOM_IN));
    
    // Remove focuable property on buttons
    for (int i = 0, n = toolBar.getComponentCount(); i < n; i++) {
      toolBar.getComponentAtIndex(i).setFocusable(false);      
    }
    
    return toolBar;
  }
  
  /**
   * Enables or disables the action matching <code>actionType</code>.
   */
  public void setEnabled(ActionType actionType, 
                         boolean enabled) {
    getActionMap().get(actionType).setEnabled(enabled);
  }
  
  /**
   * Sets the <code>NAME</code> and <code>SHORT_DESCRIPTION</code> properties value 
   * of undo and redo actions. If a parameter is null,
   * the properties will be reset to their initial values.
   */
  public void setUndoRedoName(String undoText, String redoText) {
    setNameAndShortDescription(ActionType.UNDO, undoText);
    setNameAndShortDescription(ActionType.REDO, redoText);
  }
  
  /**
   * Sets the <code>NAME</code> and <code>SHORT_DESCRIPTION</code> properties value 
   * matching <code>actionType</code>. If <code>name</code> is null,
   * the properties will be reset to their initial values.
   */
  private void setNameAndShortDescription(ActionType actionType, String name) {
    Action action = getActionMap().get(actionType);
    if (name == null) {
      name = (String)action.getValue(Action.DEFAULT);
    }
    action.putValue(Action.NAME, name);
    action.putValue(Action.SHORT_DESCRIPTION, name);
  }

  /**
   * Enables or disables transfer between components.  
   */
  public void setTransferEnabled(boolean enabled) {
    if (enabled) {
      this.catalogView.setTransferHandler(this.catalogTransferHandler);
      this.furnitureView.setTransferHandler(this.furnitureTransferHandler);
      this.planView.setTransferHandler(this.planTransferHandler);
      ((JViewport)this.furnitureView.getParent()).setTransferHandler(this.furnitureTransferHandler);
    } else {
      this.catalogView.setTransferHandler(null);
      this.furnitureView.setTransferHandler(null);
      this.planView.setTransferHandler(null);
      ((JViewport)this.furnitureView.getParent()).setTransferHandler(null);
    }
  }

  /**
   * Returns the main pane with catalog tree, furniture table and plan pane. 
   */
  private JComponent getMainPane(Home home, UserPreferences preferences, 
                                 HomeController controller) {
    JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
        getCatalogFurniturePane(controller), 
        getPlanView3DPane(home, preferences, controller));
    mainPane.setContinuousLayout(true);
    mainPane.setOneTouchExpandable(true);
    mainPane.setResizeWeight(0.3);
    return mainPane;
  }

  /**
   * Returns the catalog tree and furniture table pane. 
   */
  private JComponent getCatalogFurniturePane(HomeController controller) {
    this.catalogView = controller.getCatalogController().getView();
    JScrollPane catalogScrollPane = new HomeScrollPane(this.catalogView);
    // Add focus listener to catalog tree
    this.catalogView.addFocusListener(new FocusableViewListener(
        controller, catalogScrollPane));
    
    // Create catalog view popup menu
    JPopupMenu catalogViewPopup = new JPopupMenu();
    catalogViewPopup.add(getPopupAction(ActionType.COPY));
    catalogViewPopup.add(getPopupAction(ActionType.ADD_HOME_FURNITURE));
    this.catalogView.setComponentPopupMenu(catalogViewPopup);

    // Configure furniture view
    this.furnitureView = controller.getFurnitureController().getView();
    JScrollPane furnitureScrollPane = new HomeScrollPane(this.furnitureView);
    // Set default traversal keys of furniture view
    KeyboardFocusManager focusManager =
        KeyboardFocusManager.getCurrentKeyboardFocusManager();
    this.furnitureView.setFocusTraversalKeys(
        KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
        focusManager.getDefaultFocusTraversalKeys(
            KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
    this.furnitureView.setFocusTraversalKeys(
        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
        focusManager.getDefaultFocusTraversalKeys(
            KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));

    // Add focus listener to furniture table 
    this.furnitureView.addFocusListener(new FocusableViewListener(
        controller, furnitureScrollPane));
    // Add a mouse listener that gives focus to furniture view when
    // user clicks in its viewport
    ((JViewport)this.furnitureView.getParent()).addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent ev) {
            furnitureView.requestFocusInWindow();
          }
        });    
    
    // Create furniture view popup menu
    JPopupMenu furnitureViewPopup = new JPopupMenu();
    furnitureViewPopup.add(getPopupAction(ActionType.UNDO));
    furnitureViewPopup.add(getPopupAction(ActionType.REDO));
    furnitureViewPopup.addSeparator();
    furnitureViewPopup.add(getPopupAction(ActionType.CUT));
    furnitureViewPopup.add(getPopupAction(ActionType.COPY));
    furnitureViewPopup.add(getPopupAction(ActionType.PASTE));
    furnitureViewPopup.addSeparator();
    furnitureViewPopup.add(getPopupAction(ActionType.DELETE));
    furnitureViewPopup.add(getPopupAction(ActionType.SELECT_ALL));
    furnitureViewPopup.addSeparator();
    furnitureViewPopup.add(getPopupAction(ActionType.MODIFY_HOME_FURNITURE));
    this.furnitureView.setComponentPopupMenu(furnitureViewPopup);
    ((JViewport)this.furnitureView.getParent()).setComponentPopupMenu(furnitureViewPopup);
    
    // Create a split pane that displays both components
    JSplitPane catalogFurniturePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
        catalogScrollPane, furnitureScrollPane);
    catalogFurniturePane.setContinuousLayout(true);
    catalogFurniturePane.setOneTouchExpandable(true);
    catalogFurniturePane.setResizeWeight(0.5);
    return catalogFurniturePane;
  }

  /**
   * Returns the plan view and 3D view pane. 
   */
  private JComponent getPlanView3DPane(Home home, UserPreferences preferences, 
                                       final HomeController controller) {
    this.planView = controller.getPlanController().getView();
    final JScrollPane planScrollPane = new HomeScrollPane(this.planView);
    setPlanRulersVisible(planScrollPane, controller, preferences.isRulersVisible());
    // Add a listener to update rulers visibility in preferences
    preferences.addPropertyChangeListener("rulersVisible", 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            setPlanRulersVisible(planScrollPane, controller, (Boolean)ev.getNewValue());
          }
        });
    this.planView.addFocusListener(new FocusableViewListener(
        controller, planScrollPane));

    // Create plan view popup menu
    JPopupMenu planViewPopup = new JPopupMenu();
    planViewPopup.add(getPopupAction(ActionType.UNDO));
    planViewPopup.add(getPopupAction(ActionType.REDO));
    planViewPopup.addSeparator();
    planViewPopup.add(getPopupAction(ActionType.CUT));
    planViewPopup.add(getPopupAction(ActionType.COPY));
    planViewPopup.add(getPopupAction(ActionType.PASTE));
    planViewPopup.addSeparator();
    planViewPopup.add(getPopupAction(ActionType.DELETE));
    planViewPopup.add(getPopupAction(ActionType.SELECT_ALL));
    planViewPopup.addSeparator();
    planViewPopup.add(getWallCreationCheckBoxMenuItem(true));
    planViewPopup.add(getPopupAction(ActionType.MODIFY_HOME_FURNITURE));
    planViewPopup.add(getPopupAction(ActionType.MODIFY_WALL));
    planViewPopup.addSeparator();
    planViewPopup.add(getPopupAction(ActionType.ZOOM_OUT));
    planViewPopup.add(getPopupAction(ActionType.ZOOM_IN));
    this.planView.setComponentPopupMenu(planViewPopup);
    
    // Configure 3D view
    JComponent view3D = new HomeComponent3D(home);
    view3D.setPreferredSize(this.planView.getPreferredSize());
    view3D.setMinimumSize(new Dimension(0, 0));
    
    // Create a split pane that displays both components
    JSplitPane planView3DPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
        planScrollPane, view3D);
    planView3DPane.setContinuousLayout(true);
    planView3DPane.setOneTouchExpandable(true);
    planView3DPane.setResizeWeight(0.5);
    return planView3DPane;
  }
  
  /**
   * Sets the rulers visible in plan view.
   */
  private void setPlanRulersVisible(JScrollPane planScrollPane, 
                                    HomeController controller, boolean visible) {
    if (visible) {
      // Change column and row header views
      planScrollPane.setColumnHeaderView(
          controller.getPlanController().getHorizontalRulerView());
      planScrollPane.setRowHeaderView(
          controller.getPlanController().getVerticalRulerView());
    } else {
      planScrollPane.setColumnHeaderView(null);
      planScrollPane.setRowHeaderView(null);
    }
  }
  
  /**
   * Adds to <code>view</code> a mouse listener that disables all menu items of
   * <code>menuBar</code> during a drag and drop operation in <code>view</code>.
   */
  private void disableMenuItemsDuringDragAndDrop(JComponent view, 
                                                 final JMenuBar menuBar) {
    class MouseAndFocusListener extends MouseAdapter implements FocusListener {      
      @Override
      public void mousePressed(MouseEvent ev) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              for (int i = 0, n = menuBar.getMenuCount(); i < n; i++) {
                setMenuItemsEnabled(menuBar.getMenu(i), false);
              }
            }
          });
      }
      
      @Override
      public void mouseReleased(MouseEvent ev) {
        enableMenuItems(menuBar);
      }

      private void enableMenuItems(final JMenuBar menuBar) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              for (int i = 0, n = menuBar.getMenuCount(); i < n; i++) {
                setMenuItemsEnabled(menuBar.getMenu(i), true);
              }
            }
          });
      }

      private void setMenuItemsEnabled(JMenu menu, boolean enabled) {
        for (int i = 0, n = menu.getItemCount(); i < n; i++) {
          JMenuItem item = menu.getItem(i);
          if (item instanceof JMenu) {
            setMenuItemsEnabled((JMenu)item, enabled);
          } else if (item != null) {
            item.setEnabled(enabled 
                ? item.getAction().isEnabled()
                : false);
          }
        }
      }

      // Need to take into account focus events because a mouse released event 
      // isn't dispatched when the component loses focus  
      public void focusGained(FocusEvent ev) {
        enableMenuItems(menuBar);
      }

      public void focusLost(FocusEvent ev) {
        enableMenuItems(menuBar);
      }
    };
    
    MouseAndFocusListener listener = new MouseAndFocusListener();
    view.addMouseListener(listener);
    view.addFocusListener(listener);
  }
  
  /**
   * Displays a file chooser dialog to open a .sh3d file.
   */
  public String showOpenDialog() {
    // Use native file dialog under Mac OS X
    if (System.getProperty("os.name").startsWith("Mac OS X")) {
      return showFileDialog(false, null);
    } else {
      return showFileChooser(false, null);
    }
  }

  /**
   * Displays a file chooser dialog to save a home in a .sh3d file.
   */
  public String showSaveDialog(String name) {
    String file;
    // Use native file dialog under Mac OS X
    if (System.getProperty("os.name").startsWith("Mac OS X")) {
      file = showFileDialog(true, name);
    } else {
      file = showFileChooser(true, name);
    }
    if (file != null && !file.toLowerCase().endsWith(SWEET_HOME_3D_EXTENSION)) {
      file += SWEET_HOME_3D_EXTENSION;
    }
    return file;
  }
  
  /**
   * Displays a Swing file chooser.
   */
  private String showFileChooser(boolean save, String name) {
    JFileChooser fileChooser = new JFileChooser();
    // Choose file chooser type
    if (save && name != null) {
      fileChooser.setSelectedFile(new File(name));
    }
    // Set .sh3d files filter 
    fileChooser.setFileFilter(SWEET_HOME_3D_FILTER);
    // Update current directory
    if (currentDirectory != null) {
      fileChooser.setCurrentDirectory(currentDirectory);
    }
    int option;
    if (save) {
      option = fileChooser.showSaveDialog(this);
    } else {
      option = fileChooser.showOpenDialog(this);
    }
    if (option == JFileChooser.APPROVE_OPTION) {
      // Retrieve current directory for future calls
      currentDirectory = fileChooser.getCurrentDirectory();
      // Return selected file
      return fileChooser.getSelectedFile().toString();
    } else {
      return null;
    }
  }

  /**
   * Displays an AWT file dialog.
   */
  private String showFileDialog(boolean save, String name) {
    FileDialog fileDialog = 
      new FileDialog(JOptionPane.getFrameForComponent(this));

    // Choose file chooser type
    if (save && name != null) {
      fileDialog.setFile(new File(name).getName());
    }
    // Set .sh3d files filter 
    fileDialog.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith(SWEET_HOME_3D_EXTENSION);
        }
      });
    // Update current directory
    if (currentDirectory != null) {
      fileDialog.setDirectory(currentDirectory.toString());
    }
    if (save) {
      fileDialog.setMode(FileDialog.SAVE);
      fileDialog.setTitle(this.resource.getString("fileDialog.saveTitle"));
    } else {
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setTitle(this.resource.getString("fileDialog.openTitle"));
    }
    fileDialog.setVisible(true);
    String selectedFile = fileDialog.getFile();
    // If user choosed a file
    if (selectedFile != null) {
      // Retrieve current directory for future calls
      currentDirectory = new File(fileDialog.getDirectory());
      // Return selected file
      return currentDirectory + File.separator + selectedFile;
    } else {
      return null;
    }
  }

  /**
   * Displays <code>message</code> in an error message box.
   */
  public void showError(String message) {
    String title = this.resource.getString("error.title");
    JOptionPane.showMessageDialog(this, message, title, 
        JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays a dialog that let user choose whether he wants to overwrite 
   * file <code>name</code> or not.
   * @return <code>true</code> if user confirmed to overwrite.
   */
  public boolean confirmOverwrite(String name) {
    // Retrieve displayed text in buttons and message
    String messageFormat = this.resource.getString("confirmOverwrite.message");
    String message = String.format(messageFormat, new File(name).getName());
    String title = this.resource.getString("confirmOverwrite.title");
    String replace = this.resource.getString("confirmOverwrite.overwrite");
    String cancel = this.resource.getString("confirmOverwrite.cancel");
    
    return JOptionPane.showOptionDialog(this, message, title, 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {replace, cancel}, cancel) == JOptionPane.OK_OPTION;
  }

  /**
   * Displays a dialog that let user choose whether he wants to save
   * the current home or not.
   * @return {@link SaveAnswer#SAVE} if user choosed to save home,
   * {@link SaveAnswer#DO_NOT_SAVE} if user don't want to save home,
   * or {@link SaveAnswer#CANCEL} if doesn't want to continue current operation.
   */
  public SaveAnswer confirmSave(String name) {
    // Retrieve displayed text in buttons and message
    String messageFormat = this.resource.getString("confirmSave.message");
    String message;
    if (name != null) {
      message = String.format(messageFormat, 
          "\"" + new File(name).getName() + "\"");
    } else {
      message = String.format(messageFormat, "");
    }
    String title = this.resource.getString("confirmSave.title");
    String save = this.resource.getString("confirmSave.save");
    String doNotSave = this.resource.getString("confirmSave.doNotSave");
    String cancel = this.resource.getString("confirmSave.cancel");

    switch (JOptionPane.showOptionDialog(this, message, title, 
        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {save, doNotSave, cancel}, save)) {
      // Convert showOptionDialog answer to SaveAnswer enum constants
      case JOptionPane.YES_OPTION:
        return SaveAnswer.SAVE;
      case JOptionPane.NO_OPTION:
        return SaveAnswer.DO_NOT_SAVE;
      default : return SaveAnswer.CANCEL;
    }
  }

  /**
   * Displays a dialog that let user choose whether he wants to exit 
   * application or not.
   * @return <code>true</code> if user confirmed to exit.
   */
  public boolean confirmExit() {
    String message = this.resource.getString("confirmExit.message");
    String title = this.resource.getString("confirmExit.title");
    String quit = this.resource.getString("confirmExit.quit");
    String doNotQuit = this.resource.getString("confirmExit.doNotQuit");
    
    return JOptionPane.showOptionDialog(this, message, title, 
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {quit, doNotQuit}, doNotQuit) == JOptionPane.YES_OPTION;
  }
  
  /**
   * Displays an about dialog.
   */
  public void showAboutDialog() {
    // Use an uneditable editor pane to let user select text in dialog
    JEditorPane messagePane = new JEditorPane("text/html", 
        this.resource.getString("about.message"));
    messagePane.setOpaque(false);
    messagePane.setEditable(false);
    messagePane.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent ev) {
        if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          viewURL(ev.getURL());
        }
      }
    });
    
    String title = this.resource.getString("about.title");
    Icon   icon  = new ImageIcon(HomePane.class.getResource(
        this.resource.getString("about.icon")));
    JOptionPane.showMessageDialog(this, messagePane, title,  
        JOptionPane.INFORMATION_MESSAGE, icon);
  }

  /**
   * Launches browser with <code>url</code>.
   */
  private void viewURL(URL url) {
    try { 
      // Lookup the javax.jnlp.BasicService object 
      BasicService service = 
          (BasicService)ServiceManager.lookup("javax.jnlp.BasicService"); 
      service.showDocument(url); 
    } catch (UnavailableServiceException ex) {
      // Too bad : service is unavailable 
    } 
  }

  /**
   * Returns <code>true</code> if clipboard contains data that
   * components are able to handle.
   */
  public boolean isClipboardEmpty() {
    return !getToolkit().getSystemClipboard().
        isDataFlavorAvailable(HomeTransferableList.HOME_FLAVOR);
  }

  /**
   * A scroll pane that always displays scroll bar on Mac OS X.
   */
  private static class HomeScrollPane extends JScrollPane {
    public HomeScrollPane(JComponent view) {
      super(view);
      if (System.getProperty("os.name").startsWith("Mac OS X")) {
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
      }
    }
  }

  private static final Border UNFOCUSED_BORDER = 
    BorderFactory.createEmptyBorder(2, 2, 2, 2);
  private static final Border FOCUSED_BORDER = 
    BorderFactory.createLineBorder(UIManager.getColor("textHighlight"), 2); 

  /**
   * A focus listener that calls <code>focusChanged</code> in 
   * home controller.
   */
  private class FocusableViewListener implements FocusListener {
    private HomeController controller;
    private JComponent     feedbackComponent;
  
    public FocusableViewListener(HomeController controller, 
                                 JComponent     feedbackComponent) {
      this.controller = controller;
      this.feedbackComponent = feedbackComponent;
      feedbackComponent.setBorder(UNFOCUSED_BORDER);
    }
        
    public void focusGained(FocusEvent ev) {
      // Display a colored border
      this.feedbackComponent.setBorder(FOCUSED_BORDER);
      // Update the component used by clipboard actions
      focusedComponent = (JComponent)ev.getComponent();
      // Notify controller that active view changed
      this.controller.focusedViewChanged(focusedComponent);
    }
    
    public void focusLost(FocusEvent ev) {
      this.feedbackComponent.setBorder(UNFOCUSED_BORDER);
    }
  }
}