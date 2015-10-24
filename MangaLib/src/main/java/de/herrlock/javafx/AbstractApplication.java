package de.herrlock.javafx;

import de.herrlock.javafx.scene.SceneContainer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author HerrLock
 */
public abstract class AbstractApplication extends Application {
    private SceneContainer container;

    @Override
    public void start( final Stage stage ) {
        if ( this.container == null ) {
            throw new IllegalStateException( "need to initialize AbstractApplication.container before starting this" );
        }
        stage.setTitle( this.container.getTitle() );
        stage.getIcons().addAll( this.container.getIcons() );

        Scene scene = this.container.getScene();
        scene.getStylesheets().addAll( this.container.getStylesheets() );
        stage.setScene( scene );
        stage.show();
    }

    /**
     * Sets the SceneContainer to use, the contained Scene is shown by this Application
     * 
     * @param container
     *            the {@link SceneContainer} to use
     */
    public void setScene( final SceneContainer container ) {
        this.container = container;
    }
}
