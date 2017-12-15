/**
 * Visual representation of a snake.
 *
 * @param {*} body
 */
function Snake(body) {
  this.body = body.map(part => createVector(part.x, part.y));
  this.head = this.body[0];
  this.body.splice(0, 1);

  this.render = function() {
    rect(this.head.x, this.head.y, blockS, blockS);

    this.body.forEach(bodyPart => {
      rect(bodyPart.x, bodyPart.y, blockS, blockS);
    });
  };
}
