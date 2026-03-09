/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

const { FakeInputElement, FakeRESTService, requireBuild } = require('../helpers/fakes');

const { WebCliCommandInputComponent } = requireBuild('command-input/command-input.component.js');
const { WebCliCommandsComponent } = requireBuild('commands/commands.component.js');
const { CommunicationService } = requireBuild('service/communication.service.js');

describe('WebCLI command menu and input', function() {
  function createCommandInput(restService, communicationService) {
    const inputElement = new FakeInputElement();
    const elementRef = {
      nativeElement: {
        getElementsByTagName() {
          return [inputElement];
        }
      }
    };

    return {
      inputElement,
      component: new WebCliCommandInputComponent(restService, communicationService, elementRef)
    };
  }

  it('fills the command menu with mockup commands', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = new WebCliCommandsComponent(restService, communicationService);
    const commandGroups = [
      {
        name: 'Basic commands',
        commands: [
          {
            command: 'process resource {0}',
            help: 'Execute the commands listed in the resource file {0}.'
          },
          {
            command: 'skip on error',
            help: 'Skip execution of failed command in case of error'
          }
        ]
      }
    ];

    component.ngOnInit();
    expect(restService.getCommandsCalls).toBe(1);

    restService.emitCommandList(commandGroups);

    expect(component.commandList).toEqual(commandGroups);
  });

  it('lets the user select a command and execute it', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const commandsComponent = new WebCliCommandsComponent(restService, communicationService);
    const { component: inputComponent, inputElement } = createCommandInput(restService, communicationService);
    const selectedCommand = 'process resource {0}';

    jasmine.clock().install();

    commandsComponent.onSelect(selectedCommand);
    inputComponent.ngAfterViewChecked();

    jasmine.clock().tick(1);

    expect(inputComponent.command).toBe(selectedCommand);
    expect(inputElement.focused).toBe(true);
    expect(inputElement.selectionStart).toBe(selectedCommand.indexOf('{0}'));

    inputComponent.execute(inputComponent.command);

    jasmine.clock().uninstall();

    expect(restService.executedCommands).toEqual([selectedCommand]);
    expect(inputComponent.command).toBe('');
    expect(JSON.parse(localStorage.getItem('commandHistory'))).toEqual([selectedCommand]);
  });

  it('positions the flyout submenu on hover', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = new WebCliCommandsComponent(restService, communicationService);

    // Mock the DOM structure properly using JSDOM elements
    const submenuElement = document.createElement('div');
    submenuElement.className = 'dropdown-menu';

    const parentElement = document.createElement('div');
    parentElement.className = 'dropdown-submenu';
    parentElement.appendChild(submenuElement);

    // Override getBoundingClientRect since layout doesn't happen in JSDOM
    const parentRect = { top: 100, right: 200, bottom: 150, left: 0, width: 200, height: 50 };
    parentElement.getBoundingClientRect = () => parentRect;

    const event = {
      currentTarget: parentElement
    };

    component.onHoverSubmenu(event, []);

    expect(submenuElement.style.top).toBe(parentRect.top + "px");
    expect(submenuElement.style.left).toBe(parentRect.right + "px");
    expect(submenuElement.style.maxHeight).toBeDefined();
    expect(submenuElement.style.maxHeight).toContain("px");
  });

  it('shifts the submenu up if there is insufficient space below', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = new WebCliCommandsComponent(restService, communicationService);

    // Mock window innerHeight
    const originalInnerHeight = window.innerHeight;
    // Use Object.defineProperty to override if simple assignment fails in strict mode/jsdom
    Object.defineProperty(window, 'innerHeight', { value: 500, writable: true });

    const submenuElement = document.createElement('div');
    submenuElement.className = 'dropdown-menu';

    const parentElement = document.createElement('div');
    parentElement.className = 'dropdown-submenu';
    parentElement.appendChild(submenuElement);

    // Parent is near bottom: top=480. Space below = 500 - 480 - 10 = 10px.
    const parentRect = { top: 480, right: 200, bottom: 530, left: 0, width: 200, height: 50 };
    parentElement.getBoundingClientRect = () => parentRect;

    const event = {
      currentTarget: parentElement
    };

    // 3 items. height approx 32*3 + 10 = 106px.
    // space 10px. need 106. shift 96px.
    // newTop = 480 - 96 = 384.
    const commands = [1, 2, 3];

    component.onHoverSubmenu(event, commands);

    // Verify shift logic
    // We expect newTop < parentRect.top
    const topStyle = parseInt(submenuElement.style.top, 10);
    expect(topStyle).toBeLessThan(parentRect.top);
    // Calculated: 480 - (106 - 10) = 384.
    // Wait, itemHeight fallback is 32. 3*32+10 = 106.
    // Available = 500 - 480 - 10 = 10.
    // Shift = 106 - 10 = 96.
    // Expected top = 480 - 96 = 384.
    expect(topStyle).toBeCloseTo(384, -1); // Allow slight rounding diffs

    // Restore window
    Object.defineProperty(window, 'innerHeight', { value: originalInnerHeight, writable: true });
  });

  it('cycles through placeholders when Tab key is pressed', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const { component: inputComponent, inputElement } = createCommandInput(restService, communicationService);

    // Setup initial command with placeholders
    const commandWithPlaceholders = 'command {0} and {1}';
    inputComponent.command = commandWithPlaceholders;

    // Simulate Tab press (first time - from beginning)
    // Assuming selection starts at 0
    inputElement.selectionEnd = 0;

    // Trigger onKeyPress with Tab (keyCode 9)
    const fakeEvent = { preventDefault: jasmine.createSpy('preventDefault') };
    inputComponent.onKeyPress(fakeEvent, 9);

    expect(fakeEvent.preventDefault).toHaveBeenCalled();
    expect(inputElement.focused).toBe(true);
    // {0} starts at index 8, length 3
    expect(inputElement.selectionStart).toBe(8);
    expect(inputElement.selectionEnd).toBe(11);

    // Simulate user has selected {0} (or cursor is at 11), now press Tab again
    inputElement.selectionEnd = 11;
    inputComponent.onKeyPress(fakeEvent, 9);

    // Expect jump to {1}. index 16, length 3
    expect(inputElement.selectionStart).toBe(16);
    expect(inputElement.selectionEnd).toBe(19);

    // Press Tab again - should wrap around to {0} or stay?
    // User request: "next parameter". Usually wrapping is expected or stop.
    // Let's implement wrap around as it is standard behaviour.
    inputElement.selectionEnd = 19;
    inputComponent.onKeyPress(fakeEvent, 9);

    expect(inputElement.selectionStart).toBe(8);
    expect(inputElement.selectionEnd).toBe(11);
  });
});
