/*
 * HomeFurnitureGroup.java 4 f�vr. 2010
 *
 * Sweet Home 3D, Copyright (c) 2010 Emmanuel PUYBARET / eTeks <info@eteks.com>
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
package com.eteks.sweethome3d.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A group of furniture of furniture.
 * @since 2.3
 * @author Emmanuel Puybaret
 */
public class HomeFurnitureGroup extends HomePieceOfFurniture {
  private static final long serialVersionUID = 1L;
  
  private static final float [][] IDENTITY = new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

  private List<HomePieceOfFurniture> furniture;
  private boolean                    resizable;
  private boolean                    deformable;
  private boolean                    texturable;
  private boolean                    doorOrWindow;
  private float                      fixedWidth;
  private float                      fixedDepth;
  private float                      fixedHeight;
  private float                      dropOnTopElevation; 
  private String                     currency;
  private List<Integer>              furnitureDefaultColors;
  private List<HomeTexture>          furnitureDefaultTextures;

  private transient PropertyChangeListener furnitureListener;

  /**
   * Creates a group from the given <code>furniture</code> list. 
   * The level of each piece of furniture of the group will be reset to <code>null</code> and if they belong to levels
   * with different elevations, their elevation will be updated to be relative to the elevation of the lowest level.
   */
  public HomeFurnitureGroup(List<HomePieceOfFurniture> furniture,
                            String name) {
    this(furniture, furniture.get(0), name);
  }
  
  /**
   * Creates a group from the given <code>furniture</code> list. 
   * The level of each piece of furniture of the group will be reset to <code>null</code> and if they belong to levels
   * with different elevations, their elevation will be updated to be relative to the elevation of the lowest level.
   * The angle of the group is the one of the leading piece.
   * @since 4.5
   */
  public HomeFurnitureGroup(List<HomePieceOfFurniture> furniture,
                            HomePieceOfFurniture leadingPiece,
                            String name) {
    super(furniture.get(0));
    this.furniture = Collections.unmodifiableList(furniture); 
    
    boolean movable = true;
    this.resizable = true;
    this.deformable = true;
    this.texturable = true;
    this.doorOrWindow = true;
    boolean visible = false;
    this.currency = furniture.get(0).getCurrency();
    for (HomePieceOfFurniture piece : furniture) {
      movable &= piece.isMovable();
      this.resizable &= piece.isResizable();
      this.deformable &= piece.isDeformable();
      this.texturable &= piece.isTexturable();
      this.doorOrWindow &= piece.isDoorOrWindow();
      visible |= piece.isVisible();
      if (this.currency != null) {
        if (piece.getCurrency() == null
            || !piece.getCurrency().equals(this.currency)) {
          this.currency = null; 
        }
      }
    }
    
    setName(name);
    setNameVisible(false);
    setNameXOffset(0);
    setNameYOffset(0);
    setNameStyle(null);
    setDescription(null);
    setMovable(movable);
    setVisible(visible);
    if (this.texturable) {
      super.setColor(null);
      super.setTexture(null);
    }
    
    updateLocationAndSize(furniture, leadingPiece.getAngle(), true);
    super.setAngle(leadingPiece.getAngle());

    addFurnitureListener();
  }

  /**
   * Initializes new piece fields to their default values 
   * and reads piece from <code>in</code> stream with default reading method.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.dropOnTopElevation = -1;
    this.deformable = true;
    this.texturable = true;
    in.defaultReadObject();
    addFurnitureListener();
  }

  /**
   * Updates the location and size of this group from the furniture it contains.
   */
  private void updateLocationAndSize(List<HomePieceOfFurniture> furniture,
                                     float angle,
                                     boolean init) {
    float elevation = Float.MAX_VALUE;
    if (init) {
      // Search the lowest level elevation among grouped furniture
      Level minLevel = null;
      for (HomePieceOfFurniture piece : furniture) {
        Level level = piece.getLevel();
        if (level != null 
            && (minLevel == null
                || level.getElevation() < minLevel.getElevation())) {
          minLevel = level;
        }
      }
      for (HomePieceOfFurniture piece : furniture) {
        if (piece.getLevel() != null) {
          elevation = Math.min(elevation, piece.getGroundElevation() - minLevel.getElevation());
          // Reset piece level and elevation
          piece.setElevation(piece.getGroundElevation() - minLevel.getElevation());
          piece.setLevel(null);
        } else {
          elevation = Math.min(elevation, piece.getElevation());
        }
      }
    } else {
      for (HomePieceOfFurniture piece : furniture) {
        elevation = Math.min(elevation, piece.getElevation());
      }
    }

    float height = 0;
    float dropOnTopElevation = -1;
    for (HomePieceOfFurniture piece : furniture) {
      height = Math.max(height, piece.getElevation() + piece.getHeight());
      if (piece.getDropOnTopElevation() >= 0) {
        dropOnTopElevation = Math.max(dropOnTopElevation, 
            piece.getElevation() + piece.getHeight() * piece.getDropOnTopElevation());
      }
    }

    // Search the size of the furniture group
    AffineTransform rotation = AffineTransform.getRotateInstance(-angle);
    Rectangle2D unrotatedBoundingRectangle = null;
    for (HomePieceOfFurniture piece : getFurnitureWithoutGroups(furniture)) {
      GeneralPath pieceShape = new GeneralPath();
      float [][] points = piece.getPoints();
      pieceShape.moveTo(points [0][0], points [0][1]);
      for (int i = 1; i < points.length; i++) {
        pieceShape.lineTo(points [i][0], points [i][1]);
      }
      pieceShape.closePath();
      if (unrotatedBoundingRectangle == null) {
        unrotatedBoundingRectangle = pieceShape.createTransformedShape(rotation).getBounds2D();
      } else {
        unrotatedBoundingRectangle.add(pieceShape.createTransformedShape(rotation).getBounds2D());
      }
    }
    // Search center of the group
    Point2D center = new Point2D.Float((float)unrotatedBoundingRectangle.getCenterX(), (float)unrotatedBoundingRectangle.getCenterY());
    rotation.setToRotation(angle);
    rotation.transform(center, center);

    if (this.resizable) {
      super.setWidth((float)unrotatedBoundingRectangle.getWidth());
      super.setDepth((float)unrotatedBoundingRectangle.getHeight());
      super.setHeight(height - elevation);
    } else {
      this.fixedWidth = (float)unrotatedBoundingRectangle.getWidth();
      this.fixedDepth = (float)unrotatedBoundingRectangle.getHeight();
      this.fixedHeight = height - elevation;
    }
    this.dropOnTopElevation = (dropOnTopElevation - elevation) / height;
    super.setX((float)center.getX());
    super.setY((float)center.getY());
    super.setElevation(elevation);
  }
  
  /**
   * Adds a listener to the furniture of this group that will update the size and location 
   * of the group when its furniture is moved or resized. 
   */
  private void addFurnitureListener() {
    this.furnitureListener = new LocationAndSizeChangeListener(this);
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.addPropertyChangeListener(this.furnitureListener);
    }    
  }

  /**
   * Properties listener that updates the size and location of this group. 
   * This listener is bound to this controller with a weak reference to avoid a strong link 
   * of this group towards the furniture it contains.  
   */
  private static class LocationAndSizeChangeListener implements PropertyChangeListener { 
    private WeakReference<HomeFurnitureGroup> group;
    
    public LocationAndSizeChangeListener(HomeFurnitureGroup group) {
      this.group = new WeakReference<HomeFurnitureGroup>(group);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      // If the group was garbage collected, remove this listener from furniture
      final HomeFurnitureGroup group = this.group.get();
      if (group == null) {
        ((HomePieceOfFurniture)ev.getSource()).removePropertyChangeListener(this);
      } else if (HomePieceOfFurniture.Property.X.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.Y.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.ELEVATION.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.ANGLE.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.WIDTH.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.DEPTH.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.HEIGHT.name().equals(ev.getPropertyName())) {
        group.updateLocationAndSize(group.getFurniture(), group.getAngle(), false);
      }
    }
  }

  /**
   * Returns all the pieces of the given <code>furniture</code> list.  
   */
  private List<HomePieceOfFurniture> getFurnitureWithoutGroups(List<HomePieceOfFurniture> furniture) {
    List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>();
    for (HomePieceOfFurniture piece : furniture) {
      if (piece instanceof HomeFurnitureGroup) {
        pieces.addAll(getFurnitureWithoutGroups(((HomeFurnitureGroup)piece).getFurniture()));
      } else {
        pieces.add(piece);
      }
    }
    return pieces;
  }
  
  /**
   * Returns the furniture of this group and of all its subgroups.  
   */
  public List<HomePieceOfFurniture> getAllFurniture() {
    List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>(this.furniture);
    for (HomePieceOfFurniture piece : getFurniture()) {
      if (piece instanceof HomeFurnitureGroup) {
        pieces.addAll(((HomeFurnitureGroup)piece).getAllFurniture());
      } 
    }
    return pieces;
  }
  
  /**
   * Returns an unmodifiable list of the furniture of this group.
   */
  public List<HomePieceOfFurniture> getFurniture() {
    return Collections.unmodifiableList(this.furniture);
  }
  
  /**
   * Returns <code>null</code>.
   */
  @Override
  public String getCatalogId() {
    return null;
  }

  /**
   * Returns <code>null</code>.
   * @since 4.2
   */
  @Override
  public String getInformation() {
    return null;
  }
  
  /**
   * Returns <code>true</code> if all furniture of this group are movable.
   */
  @Override
  public boolean isMovable() {
    return super.isMovable();
  }
  
  /**
   * Sets whether this piece is movable or not.
   * @since 3.1
   */
  @Override
  public void setMovable(boolean movable) {
    super.setMovable(movable);
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.setMovable(movable);
    }
  }

  /**
   * Returns <code>true</code> if all furniture of this group are doors or windows.
   */
  @Override
  public boolean isDoorOrWindow() {
    return this.doorOrWindow;
  }

  /**
   * Returns <code>true</code> if all furniture of this group are resizable.
   */
  @Override
  public boolean isResizable() {
    return this.resizable;
  }
  
  /**
   * Returns <code>true</code> if all furniture of this group are deformable.
   * @since 3.0
   */
  @Override
  public boolean isDeformable() {
    return this.deformable;
  }
  
  /**
   * Returns <code>true</code> if all furniture of this group are texturable.
   * @since 3.5
   */
  @Override
  public boolean isTexturable() {
    return this.texturable;
  }
  
  /**
   * Returns the width of this group.
   */
  @Override
  public float getWidth() {
    if (!this.resizable) {
      return this.fixedWidth;
    } else {
      return super.getWidth();
    }
  }
  
  /**
   * Returns the depth of this group.
   */
  @Override
  public float getDepth() {
    if (!this.resizable) {
      return this.fixedDepth;
    } else {
      return super.getDepth();
    }
  }
  
  /**
   * Returns the height of this group.
   */
  @Override
  public float getHeight() {
    if (!this.resizable) {
      return this.fixedHeight;
    } else {
      return super.getHeight();
    }
  }
  
  /**
   * Returns the elevation at which should be placed an object dropped on this group.
   * @since 4.4 
   */
  @Override
  public float getDropOnTopElevation() {
    return this.dropOnTopElevation;
  }
  
  /**
   * Returns <code>null</code>.
   */
  public Content getIcon() {
    return null;
  }

  /**
   * Returns <code>null</code>.
   */
  public Content getPlanIcon() {
    return null;
  }

  /**
   * Returns <code>null</code>.
   */
  public Content getModel() {
    return null;
  }
  
  /**
   * Returns an identity matrix.
   */
  @Override
  public float [][] getModelRotation() {
    return IDENTITY;
  }

  /**
   * Returns <code>null</code>.
   * @since 3.5
   */
  @Override
  public String getStaircaseCutOutShape() {
    return null;
  }
  
  /**
   * Returns <code>null</code>.
   * @since 4.2
   */
  @Override
  public String getCreator() {
    return null;
  }
  
  /**
   * Returns the price of the furniture of this group with a price.
   */
  @Override
  public BigDecimal getPrice() {
    BigDecimal price = null;
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.getPrice() != null) {
        if (price == null) {
          price = piece.getPrice();
        } else {
          price = price.add(piece.getPrice()); 
        }
      }
    }
    if (price == null) {
      return super.getPrice();
    } else {
      return price;
    }
  }

  /**
   * Sets the price of this group.
   * @throws UnsupportedOperationException if the price of one of the pieces is set
   * @since 4.0
   */
  @Override
  public void setPrice(BigDecimal price) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.getPrice() != null) {
        throw new UnsupportedOperationException("Can't change the price of a group containing pieces with a price");
      }
    }
    super.setPrice(price);
  }
  
  /**
   * Returns the VAT percentage of the furniture of this group 
   * or <code>null</code> if one piece has no VAT percentage 
   * or has a VAT percentage different from the other furniture.
   */
  @Override
  public BigDecimal getValueAddedTaxPercentage() {
    BigDecimal valueAddedTaxPercentage = this.furniture.get(0).getValueAddedTaxPercentage();
    if (valueAddedTaxPercentage != null) {
      for (HomePieceOfFurniture piece : this.furniture) {
        BigDecimal pieceValueAddedTaxPercentage = piece.getValueAddedTaxPercentage();
        if (pieceValueAddedTaxPercentage == null
            || !pieceValueAddedTaxPercentage.equals(valueAddedTaxPercentage)) {
          return null; 
        }
      }
    }
    return valueAddedTaxPercentage;
  }
  
  /**
   * Returns the currency of the furniture of this group 
   * or <code>null</code> if one piece has no currency 
   * or has a currency different from the other furniture.
   * @since 3.5
   */
  @Override
  public String getCurrency() {
    return this.currency;
  }
  
  /**
   * Returns the VAT of the furniture of this group.
   */
  @Override
  public BigDecimal getValueAddedTax() {
    BigDecimal valueAddedTax = null;
    for (HomePieceOfFurniture piece : furniture) {
      BigDecimal pieceValueAddedTax = piece.getValueAddedTax();
      if (pieceValueAddedTax != null) {
        if (valueAddedTax == null) {
          valueAddedTax = pieceValueAddedTax;
        } else {
          valueAddedTax = valueAddedTax.add(pieceValueAddedTax);
        }
      }
    }
    return valueAddedTax;
  }
  
  /**
   * Returns the total price of the furniture of this group.
   */
  @Override
  public BigDecimal getPriceValueAddedTaxIncluded() {
    BigDecimal priceValueAddedTaxIncluded = null;
    for (HomePieceOfFurniture piece : furniture) {
      if (piece.getPrice() != null) {
        if (priceValueAddedTaxIncluded == null) {
          priceValueAddedTaxIncluded = piece.getPriceValueAddedTaxIncluded();
        } else {
          priceValueAddedTaxIncluded = priceValueAddedTaxIncluded.add(piece.getPriceValueAddedTaxIncluded());
        }
      }
    }
    return priceValueAddedTaxIncluded;
  }
  
  /**
   * Returns <code>false</code>.
   */
  @Override
  public boolean isBackFaceShown() {
    return false;
  }
  
  /**
   * Sets the <code>color</code> of the furniture of this group.
   */
  @Override
  public void setColor(Integer color) {
    super.setColor(color);
    if (color != null) {      
      storeDefaultColorsAndTextures();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setTexture(null);
        piece.setColor(color);
      } 
    } else if (getTexture() == null) {
      restoreDefaultColorsAndTextures();
    }
  }

  /**
   * Sets the <code>texture</code> of the furniture of this group.
   */
  @Override
  public void setTexture(HomeTexture texture) {
    super.setTexture(texture);
    if (texture != null) {      
      storeDefaultColorsAndTextures();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setColor(null);
        piece.setTexture(texture);
      } 
    } else if (getColor() == null) {
      restoreDefaultColorsAndTextures();
    }
  }
  
  /**
   * Stores default colors and textures. 
   */
  private void storeDefaultColorsAndTextures() {
    if (this.furnitureDefaultColors == null) {
      // Retrieve default color and texture of child furniture
      Integer [] furnitureDefaultColors = new Integer [this.furniture.size()];
      HomeTexture [] furnitureDefaultTextures = new HomeTexture [this.furniture.size()];
      for (int i = 0; i < this.furniture.size(); i++) {
        furnitureDefaultColors [i] = this.furniture.get(i).getColor();
        furnitureDefaultTextures [i] = this.furniture.get(i).getTexture();
      } 
      this.furnitureDefaultColors = Arrays.asList(furnitureDefaultColors);
      this.furnitureDefaultTextures = Arrays.asList(furnitureDefaultTextures);
    }
  }
  
  /**
   * Restores default colors and textures
   */
  private void restoreDefaultColorsAndTextures() {
    if (this.furnitureDefaultColors != null) {
      for (int i = 0; i < this.furniture.size(); i++) {
        this.furniture.get(i).setColor(this.furnitureDefaultColors.get(i));
        this.furniture.get(i).setTexture(this.furnitureDefaultTextures.get(i));
      }
      this.furnitureDefaultColors = null;
      this.furnitureDefaultTextures = null;
    }
  }

  /**
   * Sets the <code>angle</code> of the furniture of this group.
   */
  @Override
  public void setAngle(float angle) {
    if (angle != getAngle()) {
      float angleDelta = angle - getAngle();
      double cosAngleDelta = Math.cos(angleDelta);
      double sinAngleDelta = Math.sin(angleDelta);
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setAngle(piece.getAngle() + angleDelta);     
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngleDelta - (piece.getY() - getY()) * sinAngleDelta);
        float newY = getY() + (float)((piece.getX() - getX()) * sinAngleDelta + (piece.getY() - getY()) * cosAngleDelta);
        piece.setX(newX);
        piece.setY(newY);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setAngle(angle);
    }
  }
  
  /**
   * Sets the <code>abscissa</code> of this group and moves its furniture accordingly.
   */
  @Override
  public void setX(float x) {
    if (x != getX()) {
      float dx = x - getX();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setX(piece.getX() + dx);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setX(x);
    }
  }

  /**
   * Sets the <code>ordinate</code> of this group and moves its furniture accordingly.
   */
  @Override
  public void setY(float y) {
    if (y != getY()) {
      float dy = y - getY();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.addPropertyChangeListener(this.furnitureListener);
        piece.setY(piece.getY() + dy);
        piece.removePropertyChangeListener(this.furnitureListener);
      }
      super.setY(y);
    }
  }

  /**
   * Sets the <code>width</code> of this group, then moves and resizes its furniture accordingly.
   */
  @Override
  public void setWidth(float width) {
    if (width != getWidth()) {
      float widthFactor = width / getWidth();
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        float angleDelta = piece.getAngle() - angle;
        float pieceWidth = piece.getWidth();
        float pieceDepth = piece.getDepth();
        piece.setWidth(pieceWidth + pieceWidth * (widthFactor - 1) * Math.abs((float)Math.cos(angleDelta)));
        piece.setDepth(pieceDepth + pieceDepth * (widthFactor - 1) * Math.abs((float)Math.sin(angleDelta)));
        // Rotate piece to angle 0
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        // Update its abscissa
        newX = getX() + (newX - getX()) * widthFactor; 
        // Rotate piece back to its angle
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setWidth(width);
    }
  }

  /**
   * Sets the <code>depth</code> of this group, then moves and resizes its furniture accordingly.
   */
  @Override
  public void setDepth(float depth) {
    if (depth != getDepth()) {
      float depthFactor = depth / getDepth();
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        float angleDelta = piece.getAngle() - angle;
        float pieceWidth = piece.getWidth();
        float pieceDepth = piece.getDepth();
        piece.setWidth(pieceWidth + pieceWidth * (depthFactor - 1) * Math.abs((float)Math.sin(angleDelta)));
        piece.setDepth(pieceDepth + pieceDepth * (depthFactor - 1) * Math.abs((float)Math.cos(angleDelta)));
        // Rotate piece to angle 0
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        // Update its ordinate
        newY = getY() + (newY - getY()) * depthFactor;
        // Rotate piece back to its angle
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setDepth(depth);
    }
  }

  /**
   * Sets the <code>height</code> of this group, then moves and resizes its furniture accordingly.
   */
  @Override
  public void setHeight(float height) {
    if (height != getHeight()) {
      float heightFactor = height / getHeight();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setHeight(piece.getHeight() * heightFactor);
        piece.setElevation(getElevation() 
            + (piece.getElevation() - getElevation()) * heightFactor);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setHeight(height);
    }
  }

  /**
   * Sets the <code>elevation</code> of this group, then moves its furniture accordingly.
   */
  @Override
  public void setElevation(float elevation) {
    if (elevation != getElevation()) {
      float elevationDelta = elevation - getElevation();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setElevation(piece.getElevation() + elevationDelta);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setElevation(elevation);
    }
  }
  
  /**
   * Sets whether the furniture of this group should be mirrored or not.
   */
  @Override
  public void setModelMirrored(boolean modelMirrored) {
    if (modelMirrored != isModelMirrored()) {
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setModelMirrored(!piece.isModelMirrored());
        // Rotate piece to angle 0
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        // Update its abscissa
        newX = getX() - (newX - getX()); 
        // Rotate piece back to its angle
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setModelMirrored(modelMirrored);
    }
  }
  
  /**
   * Sets whether the furniture of this group should be visible or not.
   */
  @Override
  public void setVisible(boolean visible) {
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.setVisible(visible);
    }
    super.setVisible(visible);
  }

  /**
   * Set the level of this group and the furniture it contains.
   */
  @Override
  public void setLevel(Level level) {
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.setLevel(level);
    }
    super.setLevel(level);
  }
  
  /**
   * Returns <code>true</code> if one of the pieces of this group intersects
   * with the horizontal rectangle which opposite corners are at points
   * (<code>x0</code>, <code>y0</code>) and (<code>x1</code>, <code>y1</code>).
   * @since 3.5
   */
  @Override
  public boolean intersectsRectangle(float x0, float y0, 
                                     float x1, float y1) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.intersectsRectangle(x0, y0, x1, y1)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns <code>true</code> if one of the pieces of this group contains 
   * the point at (<code>x</code>, <code>y</code>)
   * with a given <code>margin</code>.
   * @since 3.5
   */
  @Override
  public boolean containsPoint(float x, float y, float margin) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.containsPoint(x, y, margin)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns a clone of this group with cloned furniture.
   */
  @Override
  public HomeFurnitureGroup clone() {
    HomeFurnitureGroup clone = (HomeFurnitureGroup)super.clone();
    // Deep clone furniture managed by this group
    clone.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture.size());
    for (HomePieceOfFurniture piece : this.furniture) {
      HomePieceOfFurniture pieceClone = piece.clone();
      clone.furniture.add(pieceClone);
      if (piece instanceof HomeDoorOrWindow
          && ((HomeDoorOrWindow)piece).isBoundToWall()) {
        ((HomeDoorOrWindow)pieceClone).setBoundToWall(true);
      }
    }
    clone.furniture = Collections.unmodifiableList(clone.furniture);
    if (this.furnitureDefaultColors != null)  {
      clone.furnitureDefaultColors = new ArrayList<Integer>(this.furnitureDefaultColors);
    }
    if (this.furnitureDefaultTextures != null) {
      clone.furnitureDefaultTextures = new ArrayList<HomeTexture>(this.furnitureDefaultTextures);
    }
    return clone;
  }
}

