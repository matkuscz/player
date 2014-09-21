/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;

/**
 *
 * @author Plutonium_
 */
public class FilterGeneratorChain<T extends FieldedValue,F extends FieldEnum<T>> extends VBox {
    
    private final List<Tuple3<String,Class,F>> data = new ArrayList();
    private final List<FilterGenerator<F>> generators = new ArrayList();
    
    private Consumer<Predicate<T>> onFilterChange;
    private Callback<Class,List<Tuple2<String,BiPredicate>>> predicateSupplier;
    private Supplier<Tuple3<String,Class,F>> prefTypeSupplier;
    private Callback<Class,Tuple2<String,BiPredicate>> prefpredicateSupplier;
    private BiFunction<F,Predicate<Object>,Predicate<T>> converter;
    
    private Predicate<T> conjuction;
    
    
    public FilterGeneratorChain() {
        this(1);
    }
    
    public FilterGeneratorChain(int i) {
        if(i<1) throw new IllegalArgumentException("Number of filters must be positive");
        for(int a=0; a<i; a++) generators.add(buildG());
    }
    
    
    public void setData(List<Tuple3<String,Class,F>> classes) {
        data.addAll(classes);
        generators.forEach(g->g.setData(classes));
    }
    
    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,F>> supplier) {
        prefTypeSupplier = supplier;
        generators.forEach(g->g.setPrefTypeSupplier(supplier));
    }
    
    public void setPrefPredicateSupplier(Callback<Class,Tuple2<String,BiPredicate>> supplier) {
        prefpredicateSupplier = supplier;
        generators.forEach(g->g.setPrefPredicateSupplier(supplier));
    }
    
    public void setPredicateSupplier(Callback<Class,List<Tuple2<String,BiPredicate>>> supplier) {
        predicateSupplier = supplier;
        generators.forEach(g->g.setPredicateSupplier(supplier));
    }
    
    public void setMapper(BiFunction<F,Predicate<Object>,Predicate<T>> mapper) {
        this.converter = mapper;
    }
    
    public void setOnFilterChange(Consumer<Predicate<T>> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    public void focus() {
        if (!generators.isEmpty()) generators.get(0).focus();
    }
    
    public boolean isEmpty() {
        return generators.stream().allMatch(FilterGenerator::isEmpty);
    }
    
    public void clear() {
        generators.forEach(FilterGenerator::clear);
        FilterGenerator first = generators.get(0);
        generators.clear();
        generators.add(first);
        generatePredicate();
        for(int i=getChildren().size()-1; i>0; i--)
            getChildren().remove(getChildren().get(i));
    }
    
    public void setButton(AwesomeIcon icon, Tooltip t, EventHandler<MouseEvent> action) {
        Label rem = AwesomeDude.createIconLabel(icon, "13");
              rem.setOnMouseClicked(action);
              rem.setPadding(new Insets(0, 3, 0, 5));
              rem.setTooltip(t);
        generators.get(0).getChildren().remove(0);
        generators.get(0).getChildren().add(0,rem);
    }

    private FilterGenerator<F> buildG() {
        FilterGenerator<F> g = new FilterGenerator();
        g.setOnFilterChange((a,b) -> generatePredicate());
        g.setPredicateSupplier(predicateSupplier);
        g.setPrefTypeSupplier(prefTypeSupplier);
        g.setPrefPredicateSupplier(prefpredicateSupplier);
        g.setData(data);
        Label rem = AwesomeDude.createIconLabel(AwesomeIcon.MINUS, "13");
        Label add = AwesomeDude.createIconLabel(AwesomeIcon.PLUS, "13");
        rem.setOnMouseClicked(e -> {
            generators.remove(g);
            getChildren().setAll(generators);
            generatePredicate();
        });
        add.setOnMouseClicked(e -> {
            int i = generators.indexOf(g);
            generators.add(i+1, buildG());
            getChildren().setAll(generators);
            generatePredicate();
        });
        g.getChildren().add(0, add);
        g.getChildren().add(0, rem);
        rem.setPadding(new Insets(0, 3, 0, 5));
        add.setPadding(new Insets(0, 3, 0, 3));
        rem.setDisable(generators.isEmpty());
        getChildren().add(generators.indexOf(g)+1,g);
        return g;
    }
    
    private void generatePredicate() {
        conjuction = o -> true;
        if(!isEmpty())
            for(FilterGenerator<F> g : generators)
                if(!g.isEmpty()) 
                    conjuction=conjuction.and(converter.apply(g.val,g.predicate));
        
        if(onFilterChange!=null) onFilterChange.accept(conjuction);
    }
}