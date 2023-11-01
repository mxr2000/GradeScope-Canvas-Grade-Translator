package gui

import model._
import model.Util.ActivationResult
import scalafx.collections.ObservableBuffer
import scalafx.scene.layout.GridPane
import scalafx.scene.control.*

case class AdjustActivationDialog(canvas: Canvas, gradeScope: GradeScope) {

  private val activationResults: ObservableBuffer[ActivationResult] =
    ObservableBuffer[ActivationResult]()

  private val btnStartActivation = new Button {
    text = "Start Activation"
    onAction = _ => {}
  }

  val grid = new GridPane {
    hgap = 10
    vgap = 10
    add(new Label(":"), 0, 0)
    add(
      new ListView[ActivationResult] {
        items = activationResults
        cellFactory = { (cell, result) =>
          cell.text = result.toString
        }
      },
      0,
      1
    )
    add(btnStartActivation, 0, 2)
  }
}
