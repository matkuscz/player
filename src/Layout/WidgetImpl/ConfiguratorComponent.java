package Layout.WidgetImpl;

import Configuration.ConfigManager;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.ItemHolders.ConfigField;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.Controller;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

@WidgetInfo(author = "Martin Polakovic",
            description = "Settings",
            version = "0.8",
            year = "2014",
            group = Widget.Group.OTHER)
public final class ConfiguratorComponent extends AnchorPane implements Controller<ClassWidget> {
        
    @FXML Button redoB;
    @FXML Accordion accordion;
    List<ConfigGroup> groups = new ArrayList<>();
    List<ConfigField> configFields = new ArrayList<>();    
    private Configuration old_config;
    
    @IsConfig(name = "Show non editable fields", info = "Include non read-only fields.")
    public boolean show_noneditable = false;
    @IsConfig(name = "Field names alignment", info = "Alignment of field names.")
    public HPos alignemnt = HPos.RIGHT;
    @IsConfig(name = "Group titles alignment", info = "Alignment of group names.")
    public Pos title_align = Pos.CENTER;
    @IsConfig(visible = false)
    public String expanded = "";
    
    public ConfiguratorComponent() {
        FXMLLoader fxmlLoader = new FXMLLoader(ConfiguratorComponent.class.getResource("Configurator.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new RuntimeException("ConfiguratorComponent source data coudlnt be read.");
        }
        
        // consume scroll event to prevent other scroll behavior // optional
        setOnScroll(Event::consume);
    }
    
    @FXML
    public void initialize() {
        old_config = Configuration.getCurrent();
        redoB.setDisable(true);
        
        // clear previous fields
        configFields.clear();
        accordion.getPanes().clear();
        groups.forEach(g->g.grid.getChildren().clear());
        groups.forEach(g->g.grid.getRowConstraints().clear());
        
        // sort & populate fields
        old_config.getFields().stream()
        .sorted((o1,o2) -> o1.gui_name.compareToIgnoreCase(o2.gui_name)).forEach(f-> {
            ConfigGroup g = getGroup(f.group);                                  // find group
            ConfigField cf = ConfigField.create(f);                             // create
            if (cf != null && !(!show_noneditable && !f.editable)) {            // ignore on fail || noneditabe
                configFields.add(cf);
                g.grid.getRowConstraints().add(new RowConstraints());
                g.grid.add(cf.getLabel(), 1, g.grid.getRowConstraints().size());  
                g.grid.add(cf.getControl(), 2, g.grid.getRowConstraints().size());
            }
        });
        
        // sort & populate groups
        groups.stream().sorted((o1,o2) -> o1.name().compareTo(o2.name()))
                       .forEach(g -> accordion.getPanes().add(g.pane));
        
        // expand     
        for(ConfigGroup g: groups) 
            if(g.name().equals(expanded))
                accordion.setExpandedPane(g.pane);
        
    }
    
    @FXML
    private void ok() {
        Configuration new_config = Configuration.getCurrent();
        for (ConfigField f: configFields)
            if (f.hasValue()) 
                new_config.setField(f.getName(), f.getValue());
        
        boolean changed = ConfigManager.change(new_config);     // change
        if (changed) {
            refresh();
            redoB.setDisable(false);                            // enable 
        }     
    }
    
    @FXML
    private void redo() {
        ConfigManager.change(old_config);
        refresh();
        redoB.setDisable(true);
    }
    
    @FXML
    private void defaults() {
        ConfigManager.setToDefault();
        refresh();
    }

/******************************************************************************/
    
    private ClassWidget w;
    
    @Override public void refresh() {
        initialize();
        groups.forEach(g -> g.grid.getColumnConstraints().get(1).setHalignment(alignemnt));
        groups.forEach(g -> g.pane.setAlignment(title_align));
        
//        // expand remembered
//        for(ConfigGroup g: groups) 
//            if(g.name().equals(expanded))
//                accordion.setExpandedPane(g.pane);
    }

    @Override public void setWidget(ClassWidget w) {
        this.w = w;
    }

    @Override public ClassWidget getWidget() {
        return w;
    }

/******************************************************************************/
    
    private final class ConfigGroup {
        final TitledPane pane = new TitledPane();
        final GridPane grid = new GridPane();
        
        public ConfigGroup(String name) {
            pane.setText(name);
            pane.setContent(grid);
            pane.expandedProperty().addListener((o,old,nw) -> {
                if(nw) expanded=pane.getText();
            });
            
            grid.setVgap(3);
            grid.setHgap(10);
            
            ColumnConstraints gap = new ColumnConstraints(20);
            ColumnConstraints c1 = new ColumnConstraints(200);
                              c1.setMaxWidth(-1);
                              c1.setMinWidth(-1);
                              c1.setPrefWidth(-1);
                              c1.setHgrow(Priority.ALWAYS);
                              c1.setFillWidth(true);
            ColumnConstraints c2 = new ColumnConstraints(200);
                              c2.setMaxWidth(-1);
                              c2.setMinWidth(-1);
                              c2.setPrefWidth(-1);
                              c2.setHgrow(Priority.ALWAYS);
                              c2.setFillWidth(true);
            grid.getColumnConstraints().addAll(gap,c1,c2);
            
            groups.add(this);
        } 
        
        public String name() {
            return pane.getText();
        }
    }
    
    private ConfigGroup getGroup(String category) {
        for (ConfigGroup g: groups) {
            if(g.name().equals(category))
                return g;
        }
        return new ConfigGroup(category);
    }
    
}